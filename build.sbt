name := """EventExperts"""

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.6"


libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "org.apache.spark" % "spark-streaming_2.10" % "1.5.1",
  "org.apache.spark" % "spark-streaming-twitter_2.10" % "1.5.1",
  "net.debasishg" %% "redisclient" % "3.0",
  "com.github.nscala-time" %% "nscala-time" % "2.4.0",
  "org.mongodb" %% "casbah" % "3.0.0",
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.13",
  "net.sf.jazzy" % "jazzy" % "0.5.2-rtext-1.4.1-2",
  "org.scalatest" % "scalatest_2.10" % "3.0.0-M15",
  "org.spire-math" %% "jawn-parser" % "0.8.3",
  "org.spire-math" %% "jawn-ast" % "0.8.3"
)


dependencyOverrides ++= Set(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.4.4",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.4.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.4.4",
  "org.twitter4j" % "twitter4j-core" % "3.0.4",
  "org.twitter4j" % "twitter4j-stream" % "3.0.4"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
