name := "kafka"

version := "0.1"

scalaVersion := "2.12.10"

val sparkVersion = "3.1.1"

lazy val root = (project in file(".")).
  settings(
    name := "kafka",
    version := "1.0",
    scalaVersion := "2.12.10",
    mainClass in Compile := Some("kafka")
  )

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
resolvers += "Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven"


libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % "2.12.10" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.1.1" % "provided",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.1.1" % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.1.1" % "provided",
  "org.apache.spark" %% "spark-core" % "3.1.1" % "provided",
  "org.apache.spark" %% "spark-streaming" % "3.1.1" % "provided",
  "com.typesafe" % "config" % "1.3.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models"
)

libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"
libraryDependencies += "graphframes" % "graphframes" % "0.8.1-spark2.4-s_2.12"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}