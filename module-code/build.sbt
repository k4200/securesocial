import play.sbt.PlayImport.PlayKeys._

// import SonatypeKeys._

// Import default settings. This changes `publishTo` settings to use the Sonatype repository and add several commands for publishing.
// sonatypeSettings

name := "SecureSocial"

version := Common.version

scalaVersion := Common.scalaVersion
crossScalaVersions := Common.crossScalaVersions

libraryDependencies ++= Seq(
  ws,
  filters,
  specs2 % "test",
  ehcache, // should this be cacheApi and we allow the user to specify their own cache?
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1", // this could be play-mailer but wouldn't have the guice module
  "io.methvin.play" %% "autoconfig-macros" % "0.2.0" % "provided",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

organization := "tv.kazu"

organizationName := ""

organizationHomepage := Some(new URL("http://kazu.tv"))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := sonatypePublishTo.value

startYear := Some(2012)

description := "An authentication module for Play Framework applications supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://www.securesocial.ws"))

pomExtra := (
  <scm>
    <url>https://github.com/k4200/securesocial</url>
    <connection>scm:git:git@github.com:k4200/securesocial.git</connection>
    <developerConnection>scm:git:https://github.com/k4200/securesocial.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>k4200</id>
      <name>KASHIMA Kazuo</name>
      <email>k4200 [at] kazu.tv</email>
      <url>https://twitter.com/k4200</url>
    </developer>
  </developers>
)

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature", "-Xmax-classfile-name","78")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8",  "-Xlint:-options", "-Xlint:unchecked", "-Xlint:deprecation" )

// packagedArtifacts += ((artifact in playPackageAssets).value -> playPackageAssets.value)

routesImport += "securesocial.controllers.Implicits._"