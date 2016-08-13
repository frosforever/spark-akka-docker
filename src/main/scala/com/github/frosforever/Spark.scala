package com.github.frosforever

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.apache.spark.{SparkConf, SparkContext, SparkFiles}

import scala.io.Source

object Web extends App {
  implicit val system = ActorSystem("spark-akka-http")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  val route =
    path("run" / Remaining) { str =>
      get {
        complete {
          val words = Spark.findCount(str)
          s"$str showed up in ${words.length} many words. ${words.mkString(",")}"
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 9000)

  println(
    s"""
       |Spark running local: ${Spark.sc.isLocal}
       |Spark master: ${Spark.sc.master}
       |Server online at http://localhost:9000/
       |Spark UI available at http://localhost:4050
       |""".stripMargin)
}

/**
  * Put Spark stuff in separate object with just what it needs so you don't have
  * to have any extra jars besides `this` on the cluster classpath.
  * Might be a bit more tedious if things are more complicated then something so simple
  */
object Spark {
  // just a way of indicating local vs cluster mode with an env var
  val envVar = scala.util.Properties.envOrNone("AKKA_SPARK_MASTER")
  val master = envVar.fold {
    println("----using default local master----")
    "local"
  }{s =>
    println(s"---- using env var $s")
    s
  }

  val sparkConf = new SparkConf()
    .setAppName("spark-akka-docker")
    //Gotta get the jars in there. If there are external dependencies then this won't suffice
    .setJars(SparkContext.jarOfClass(this.getClass).toSeq)
    //Set to non default spark UI port for ease with docker
    .set("spark.ui.port", "4050")
    .setMaster(master)
  val sc = new SparkContext(sparkConf)

  /*

  Not sure why this wasn't working across the cluster but I kept getting FileNotFound.
  This does work in single standalone mode

  sc.addFile("https://wordpress.org/plugins/about/readme.txt")
  val lines = sc.textFile(SparkFiles.get("readme.txt")).cache()

  In a more complicated app when pulling from s3 consider using the build in s3 driver
  (which may not parallelize) correctly. Or doing something a bit more manual with the s3 sdk:

  val request = new ListObjectsRequest()
    request.setBucketName(bucket)
    request.setPrefix(prefix)
    request.setMaxKeys(pageLength)
    def s3 = new AmazonS3Client(new BasicAWSCredentials(key, secret))

    val objs = s3.listObjects(request) // Note that this method returns truncated data if longer than the "pageLength" above. You might need to deal with that.
    sc.parallelize(objs.getObjectSummaries.map(_.getKey).toList)
        .flatMap { key => Source.fromInputStream(s3.getObject(bucket, key).getObjectContent: InputStream).getLines }

    Though this does have the resource pressure of loading up into memory and also might leave the stream unclosed.
    Investigate if this is something useful.

  */

  val linesRaw = Source.fromURL("https://wordpress.org/plugins/about/readme.txt").mkString.lines.toList
  val lines = sc.parallelize(linesRaw).cache()


  def findCount(str: String) = {
    lines
      .flatMap(line => line.split(" "))
      .filter(_.contains(str))
      .collect()
  }
}
