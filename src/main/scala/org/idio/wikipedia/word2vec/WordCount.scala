package org.idio.wikipedia.word2vec

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Count the total number of words, mainly for testing, roughly equivalent to `wc -w`
 *
 * For enwiki-20150205, the word count is around: 3,814,140,429
 */
object WordCount {

  def main(args: Array[String]): Unit = {
    val pathToReadableWikipedia = "file://" + args(0)

    println("Path to Readable Wikipedia: " + pathToReadableWikipedia)

    val conf = new SparkConf()
      .setMaster("local[8]")
      .setAppName("WordCount")
      .set("spark.executor.memory", "11G")

    implicit val sc: SparkContext = new SparkContext(conf)

    val readableWikipedia = sc.textFile(pathToReadableWikipedia)

    // split each line into title, text pair
    val wikiTitleTexts = readableWikipedia.map { line =>
      val splitLine = line.split("\t")
      try {
        // id, title, redirect, text
        val wikiTitle = splitLine(1)
        val redirect = splitLine(2)
        val articleText = splitLine(3)
        (wikiTitle, articleText)
      } catch {
        case _: Exception => ("", "")
      }
    }

    val count = wikiTitleTexts map {
      case (title, text) =>
        text.split("\\s").size.toLong
    } reduce (_ + _)

    println(s"final word count: $count")
  }
}


