assemblySettings

name := "wiki2vec"

version := "1.0"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  "opennlp sourceforge repo" at "http://opennlp.sourceforge.net/maven2"
)

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "16.0.1",
  "xerces" % "xercesImpl" % "2.11.0",
  "com.github.jponge" % "lzma-java" % "1.3",
  "org.apache.commons" % "commons-compress" % "1.5",
  "commons-compress" % "commons-compress" % "20050911",
  "org.apache.spark" %% "spark-core" % "1.2.0" % "provided",
  "com.bizo" % "mighty-csv_2.10" % "0.2",
  "net.debasishg" %% "redisclient" % "2.13",
  "org.scalanlp" %% "chalk" % "1.3.2" exclude("com.typesafe.sbt", "sbt-pgp"),
  "org.apache.opennlp" % "opennlp-tools" % "1.5.2-incubating",
  "com.github.rholder" % "snowball-stemmer" % "1.3.0.581.1"
)
