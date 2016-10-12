name := "maze"
organization := "com.vsct.dt"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.0-RC1-ceaf419"


libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.12.0-RC1" % "2.4.10",
  "org.scala-lang.modules" % "scala-xml_2.12.0-RC1" % "1.0.5",
  ("org.scala-lang.modules" % "scala-parser-combinators_2.12.0-RC1" % "1.0.4").exclude("org.scala-lang", "scala-library"),
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "com.github.docker-java" % "docker-java" % "3.0.6",
  "com.google.code.findbugs" % "jsr305" % "3.0.1",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.12.0-RC1" % "2.8.3",
  "org.scalatest" % "scalatest_2.12.0-RC1" % "3.0.0",
  "net.sf.saxon" % "Saxon-HE" % "9.6.0-5"
)


resolvers += "nexus at vsct" at "http://nexus/content/groups/public/"