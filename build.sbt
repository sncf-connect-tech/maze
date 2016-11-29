/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

moduleName := "maze"
organization := "fr.vsct.dt"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "com.github.docker-java" % "docker-java" % "3.0.6",
  "com.google.code.findbugs" % "jsr305" % "3.0.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.4",
  "org.scalatest" %% "scalatest" % "3.0.0",
  "net.sf.saxon" % "Saxon-HE" % "9.6.0-5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7" % "optional"
)

scalastyleConfig := file("project/scalastyle-config.xml")
scalastyleFailOnError := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value
