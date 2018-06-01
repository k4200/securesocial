name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

scalariformSettings

libraryDependencies ++= Seq(
  specs2 % "test",
  "tv.kazu" %% "securesocial" % version.value
)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

routesImport ++= Seq("scala.language.reflectiveCalls")
