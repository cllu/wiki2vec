package org.idio.wikipedia.word2vec

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

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
        val wikiTitle = splitLine(0)
        val articleText = splitLine(1)
        (wikiTitle, articleText)
      } catch {
        case _: Exception => ("", "")
      }
    }

    val count = wikiTitleTexts map {
      case (title, text) =>
        text.split("\\s").size
    } reduce (_ + _)

    println(s"final word count: $count")
  }
}


