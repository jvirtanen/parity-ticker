name := "parity-ticker"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

val parityVersion = "0.3.0"

libraryDependencies ++= Seq(
  "com.paritytrading.foundation" % "foundation"    % "0.1.0",
  "org.jvirtanen.config"         % "config-extras" % "0.1.0",
  "org.jvirtanen.parity"         % "parity-net"    % parityVersion,
  "org.jvirtanen.parity"         % "parity-top"    % parityVersion,
  "org.jvirtanen.parity"         % "parity-util"   % parityVersion
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
