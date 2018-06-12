name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies ++= Seq(
  specs2 % "test",
  "tv.kazu" %% "securesocial" % version.value,
  guice
)

resolvers += Resolver.sonatypeRepo("snapshots")

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

routesImport ++= Seq("scala.language.reflectiveCalls")
