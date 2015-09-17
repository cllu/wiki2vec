package org.idio.wikipedia.word2vec

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.idio.wikipedia.dumps.{EnglishWikipediaPage, WikipediaPage}
import org.idio.wikipedia.redirects.{EmptyRedirectStore, MapRedirectStore, RedirectStore}
import org.idio.wikipedia.utils.{NoStemmer, SnowballStemmer}

/**
 * Creates a corpus which can feed to word2vec
 * to extract vectors for each wikipedia topic.
 *
 * It assumes a "ReadableWikipedia " dump is fed.
 * A Readable Wikipedia dump is defined as one in which every line in the file follows:
 * [Article Title] [Tab] [Article Text]
 */
class Word2VecCorpus(pathToReadableWiki: String, redirectStore: RedirectStore, pathToOutput: String, language: String)(implicit val sc: SparkContext) {

  private val PREFIX = "w:"

  // RDD of a readableWikipedia where each line follows the format :
  // id title redirect text (separated by TAB)
  private val readableWikipedia = sc.textFile(pathToReadableWiki)

  // RDD (title, text)
  private val wikiTitleTexts = getPairRDD(readableWikipedia)

  private val redirectStoreBC = sc.broadcast(redirectStore)

  /**
   * Returns a PairRDD (title, text)
   * Out of a readable wikipedia
   */
  private def getPairRDD(articlesLines: RDD[String]) = {
    articlesLines.map { line =>
      val splitLine = line.split("\t")
      try {
        // id, title, redirect, text
        val wikiTitle = splitLine(1)
        val articleText = splitLine(3).replace("\\\\n", "\n").replace("\\\\t", "\t")
        (wikiTitle, articleText)
      } catch {
        case _: Exception => ("", "")
      }
    }
  }

  /*
  *  Clean the articles (MediaWiki markup)
  * */
  private def cleanArticles(titleArticleText: RDD[(String, String)]) = {

    titleArticleText.map {
      case (title, text) =>
        // Removes all of the MediaWiki boilerplate, so we can get only the article's text
        val wikiModel = new EnglishWikipediaPage()

        // cleans MediaWiki markup
        val pageContent = WikipediaPage.readPage(wikiModel, text)

        // cleans further Style tags {| some CSS inside |}
        val markupClean = ArticleCleaner.cleanStyle(pageContent)

        // clean brackets i.e: {{cite}}
        val cleanedText = ArticleCleaner.cleanCurlyBraces(markupClean)

        // cleans "==" out of "==Title=="
        val noTitleMarkers = cleanedText.replace("=", " ").replace("*", " ")

        (title, noTitleMarkers)
    }

  }

  /**
   * Replaces links to wikipedia articles following the format:
   *
   * `[[Wikipedia Title]]` =>  w:Wikipedia_Title <Space> Wikipedia Title
   * `[[Wikipedia Title | anchor]]` => w:Wikipedia_Title <Space> anchor
   **/
  private def replaceLinksForIds(titleArticleText: RDD[(String, String)], redirectStore: RedirectStore) = {

    // avoiding to serialize this class for Spark
    val prefix = PREFIX.toString

    val replace = { (anchorText: String, dbpdiaId: String) =>
      // Avoiding serializing this for spark..
      " " + prefix + dbpdiaId + " " + anchorText + " "
    }
    // replaces {{Wikipedia Title}} => w:Wikipedia_Title
    val redirectStore_local = redirectStoreBC.value
    titleArticleText.map { case (title, text) =>
      (title, ArticleCleaner.replaceLinks(text, replace, redirectStore_local))
    }
  }

  /**
   * Dumb Tokenization.
   * Replace this for something smarter
   */
  private def tokenize(stringRDD: RDD[(String, String)]): RDD[String] = {

    val prefix = PREFIX
    val language_local = language

    val tokenizedLines = stringRDD.map {
      case (dbpedia, line) =>
        val stemmer = try {
          new SnowballStemmer(language_local)
        } catch {
          case _: Exception => new NoStemmer()
        }
        line.split("\\s").map {
          case w if w.startsWith(prefix) => w
          case word => stemmer.stem(word.toLowerCase.replace(",", "").replace(".", "").replace("“", "").replace("\\", "").replace("[", "").replace("]", "").replace("‘", ""))
        }.mkString(" ")
    }
    tokenizedLines
  }

  def getWord2vecCorpus(): Unit = {
    val replacedLinks = replaceLinksForIds(wikiTitleTexts, redirectStore)
    val cleanedArticles = cleanArticles(replacedLinks)
    val tokenizedArticles = tokenize(cleanedArticles)
    tokenizedArticles.saveAsTextFile(pathToOutput)
  }
}

object Word2VecCorpus {

  def main(args: Array[String]): Unit = {
    val pathToReadableWikipedia = "file://" + args(0)
    val pathToRedirects = args(1)
    val pathToOutput = "file://" + args(2)
    val language = try {
      args(3)
    } catch {
      case _: Exception =>
        println("Warning: Stemming is deactivated..")
        "NoStemmer"
    }

    println("Path to Readable Wikipedia: " + pathToReadableWikipedia)
    println("Path to Wikipedia Redirects: " + pathToRedirects)
    println("Path to Output Corpus : " + pathToOutput)

    val conf = new SparkConf()
      .setMaster("local[8]")
      .setAppName("Wiki2Vec corpus creator")
      .set("spark.executor.memory", "11G")

    implicit val sc: SparkContext = new SparkContext(conf)

    val redirectStore = try {
      val redirects = new MapRedirectStore(pathToRedirects)
      redirects
    } catch {
      case e: Exception =>
        e.printStackTrace()
        println("using empty redirect store..")
        new EmptyRedirectStore(pathToRedirects)
    }

    val word2vecCorpusCreator = new Word2VecCorpus(pathToReadableWikipedia, redirectStore, pathToOutput, language)
    word2vecCorpusCreator.getWord2vecCorpus()
  }
}
