import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala._


object StreamProcessingFlinkEngine {
  def main(args: Array[String]): Unit = {

    if (args.length < 4) {
      println("Usage: Main <kafka-topic> <kafka-bootstrap-servers> <processing-interval> <alert-threshold> <url-alerting>")
      System.exit(1)
    }

    val kafkaTopic = args(0)
    val kafkaBootstrapServers = args(1)
    val alertThreshold = args(2).toInt
    val urlAlerting = args(3)

    // Adding flink execution environment
    val env = StreamExecutionEnvironment.createLocalEnvironment()

    // Adding KafkaSource
    val kafkaSource = KafkaSource.builder[String]()
      .setBootstrapServers(kafkaBootstrapServers)
      .setTopics(kafkaTopic)
      .setGroupId("flink-consumer-group")
      .setStartingOffsets(OffsetsInitializer.latest())
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()

    // Consuming from Kafka
    val jsonStr = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")

    // Deserialize JSON to object for each element
    val deserializedReports = jsonStr.flatMap(json => IoTReport.fromJson(json))

    val highDecibelReports = deserializedReports.filter(_.dB > alertThreshold).map { report =>
      println(s"High decibel report: $report")
      val bot = new Bot(urlAlerting)


      bot.sendAlert(report.timestamp.toString, report.studentId, report.moduleId, report.latitude, report.longitude, report.dB)
    }

    // Execute the Flink job
    env.execute("Read from Kafka")
  }

}
