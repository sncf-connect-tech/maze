startYear := Some(2015)
description := """Maze helps you automate your technical tests"""

licenses := Seq("Apache 2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0"))

organizationName := "VSCT"
organizationHomepage := Some(url("http://www.voyages-sncf.com"))

scmInfo := Some(ScmInfo(
  browseUrl = url("http://github.com/voyages-sncf-technologies/maze/tree/master"),
  connection = "scm:git:git://github.com/voyages-sncf-technologies/maze.git",
  devConnection = Some("scm:git:ssh://github.com:voyages-sncf-technologies/maze.git")))

developers := List (
  Developer(
    id = "flaroche",
    name = "François LAROCHE",
    email = "flaroche.prestataire@voyages-sncf.com",
    url = url("http://www.github.com/larochef")
  )
)

homepage := Some(url("http://github.com/voyages-sncf-technologies/maze/tree/master"))
name := s"${organization.value}:${moduleName.value}"
pomIncludeRepository := {repo => false}
