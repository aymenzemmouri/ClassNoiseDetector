import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 5) {
      println("Usage: Main <kafka-topic> <kafka-bootstrap-servers> <processing-interval> <storage-mode> <hdfs-base-path>")
      System.exit(1)
    }

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.apache.kafka").setLevel(Level.WARN)

    val kafkaTopic = args(0)
    val kafkaBootstrapServers = args(1)
    val processingInterval = args(2)
    val storageMode = args(3)
    val hdfsBasePath = args(4)

    val pattern = storageMode match {
      case "day" => "dd-MM-yyyy"
      case "month" => "MM-yyyy"
      case "year" => "yyyy"
      case "hour" => "dd-MM-yyyy-HH"
      case _ => "dd-MM-yyyy"
    }
    //print all the arguments
    println("kafkaTopic: " + kafkaTopic)
    println("kafkaBootstrapServers: " + kafkaBootstrapServers)
    println("Processing interval: " + processingInterval)
    println("HDFS path: " + hdfsBasePath)
    println("Storage mode : " + pattern)

    val conf = new SparkConf()
      .setAppName("Batch processing")
      .setMaster("local[*]")
      .set("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")

    // Create a SparkSession
    val spark = SparkSession.builder()
      .config(conf)
      .getOrCreate()


    // Import implicits
    import spark.implicits._
 
    // Set the logging level to WARN for the Kafka client classes
    LogManager.getLogger("org.apache.kafka").setLevel(Level.WARN)
    LogManager.getLogger("kafka").setLevel(Level.WARN)

    System.setProperty("HADOOP_USER_NAME", "root")

    // Read data from Kafka and deserialize JSON to IoTReport
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", kafkaTopic)
      .load()
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .as[(String, String)]
      .flatMap { case (_, json) =>
        IoTReport.fromJson(json)
      }
      .toDF()

    // Process the data received from Kafka
    val query = df
      .writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", false)
      .option("escape", false)
      .trigger(org.apache.spark.sql.streaming.Trigger.ProcessingTime(processingInterval))
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        val currentDateTime = LocalDateTime.now()
        val formattedDateTime = currentDateTime.format(DateTimeFormatter.ofPattern(pattern))

        val hdfsPath = s"$hdfsBasePath/$formattedDateTime"

        batchDF.write
          .format("parquet")
          .mode("append")
          .save(hdfsPath)
      }
      .start()

    query.awaitTermination()

    spark.stop()
  }
}

