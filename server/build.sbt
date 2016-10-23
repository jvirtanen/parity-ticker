name := "parity-ticker"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

val parityVersion = "0.5.0"

libraryDependencies ++= Seq(
  "com.paritytrading.foundation" % "foundation"    % "0.2.0",
  "com.paritytrading.parity"     % "parity-net"    % parityVersion,
  "com.paritytrading.parity"     % "parity-top"    % parityVersion,
  "com.paritytrading.parity"     % "parity-util"   % parityVersion,
  "org.jvirtanen.config"         % "config-extras" % "0.1.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
