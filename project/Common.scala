

object Common {
  def version = "3.3.0-SNAPSHOT"
  def playVersion = System.getProperty("play.version", "2.6.13")
  def scalaVersion = System.getProperty("scala.version", "2.12.4")
  def crossScalaVersions = Seq(scalaVersion, "2.11.12")
}
