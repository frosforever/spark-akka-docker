name := "spark-akka-docker"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.spark"  %% "spark-core" % "2.0.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-stream" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-testkit" % "2.4.8"
)

enablePlugins(JavaAppPackaging)

dockerBaseImage := "java:8"

dockerExposedPorts := Seq(9000, 4050)

javaOptions in Universal ++= Seq(
  "-J-Xmx1024m",
  "-J-Xms1024m"
)
