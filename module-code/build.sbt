import play.sbt.PlayImport.PlayKeys._

// import SonatypeKeys._

// Import default settings. This changes `publishTo` settings to use the Sonatype repository and add several commands for publishing.
// sonatypeSettings

name := "SecureSocial"

version := Common.version

scalaVersion := Common.scalaVersion

crossScalaVersions := Seq("2.11.8")

//PlayKeys.generateRefReverseRouter := false

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % "test",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

scalariformSettings

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

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