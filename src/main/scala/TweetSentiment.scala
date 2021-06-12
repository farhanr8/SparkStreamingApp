import java.sql.Timestamp
import java.util.Properties

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.window
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.streaming.Trigger._

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import org.apache.log4j.{Level, Logger}
import org.apache.spark.storage._
import scala.collection.convert.wrapAll._
import scala.io.Source

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations


object Analysis extends Enumeration {
  type Sentiment                  = Value
  val POSITIVE, NEGATIVE, NEUTRAL = Value

  def toSentiment(sentiment: Int): Sentiment = sentiment match {
    case x if x == 0 || x == 1 => Analysis.NEGATIVE
    case 2 => Analysis.NEUTRAL
    case x if x == 3 || x == 4 => Analysis.POSITIVE
  }
}


object SentimentAnalysis {
  val properties = new Properties()
  properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(properties)

  def mainSentiment(input: String): Analysis.Sentiment = Option(input) match {
    case Some(text) if !text.isEmpty => extractSentiment(text)
    case _ => throw new IllegalArgumentException("input can't be null or empty")
  }

  private def extractSentiment(text: String): Analysis.Sentiment = {
    val (_, sentiment) = extraction(text)
      .maxBy { case (tweets, _) => tweets.length }
    sentiment
  }

  def extraction(text: String): List[(String, Analysis.Sentiment)] = {
    val annotation: Annotation = pipeline.process(text)
    val tweetSentences         = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    tweetSentences
      .map(tweets => (tweets, tweets.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map { case (tweets, tree) => (tweets.toString,Analysis.toSentiment(RNNCoreAnnotations.getPredictedClass(tree))) }
      .toList
  }
}


object TweetSentiment {
  def main(args: Array[String]): Unit = {

    if (args.length < 2) {
      println("Correct usage: TweetSentiment topicName [filters]")
      System.exit(1)
    }

    // Logger
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    // Set Twitter Variables
    val topic = args(0)
    val filters = args.slice(1, args.length)
    val path = getClass.getResource("/app.properties")
    val properties: Properties = new Properties()
    if (path != null) {
      val source = Source.fromURL(path)
      properties.load(source.bufferedReader())
    }
    else {
      rootLogger.error("properties file cannot be loaded at path " + path)
      throw new java.io.FileNotFoundException("Properties file cannot be loaded")
    }
    val consumerKey = properties.getProperty("consumerKey")
    val consumerSecret = properties.getProperty("twitter_consumerSecret")
    val accessToken = properties.getProperty("twitter_accessToken")
    val accessTokenSecret = properties.getProperty("twitter_accessTokenSecret")
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    // Spark Configuration
    val conf = new SparkConf().setAppName("Tweet Sentiments")
                              .setMaster("local[*]")
                              .set("spark.driver.host", "localhost")
    val streamContext = new StreamingContext(conf, Seconds(5))

    // Get twitter stream
    val tweetStream = TwitterUtils.createStream(streamContext, None, filters)
    val en_tweets = tweetStream.filter(_.getLang() == "en")
    val tweets = en_tweets.map(status => (status.getText()))

    // Send twitter stream to kafka broker
    tweets.foreachRDD { (rdd, time) =>
      rdd.foreachPartition { partition =>
        val kafkaProperties = new Properties()
        val bootstrap_host = "localhost:9092"
        kafkaProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        kafkaProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        kafkaProperties.put("bootstrap.servers", bootstrap_host)
        val kafkaProducer = new KafkaProducer[String, String](kafkaProperties)

        partition.foreach { element =>
          val data = element.toString()
          val analysis = SentimentAnalysis.mainSentiment(data).toString()
          val output = new ProducerRecord[String, String](topic, "sentiment", analysis + "->" +  data)
          kafkaProducer.send(output)
        }

        kafkaProducer.flush()
        kafkaProducer.close()
      }
    }

    streamContext.start()
    streamContext.awaitTermination()

  }
}
