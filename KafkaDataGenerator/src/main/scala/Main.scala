import java.util.Properties
import scala.concurrent.duration._
import java.time._
import scala.concurrent.{Future, Await}
import akka.actor.{ActorSystem, Cancellable}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import models.{StudentData, SensorData}
import upickle.default._
import scala.util.{Random, Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    // Ensure correct number of arguments
    if (args.length < 2) {
      println("Usage: Main <kafka-topic> <kafka-bootstrap-servers>")
      System.exit(1)
    }

    val kafkaTopic = args(0)
    val kafkaBootstrapServers = args(1)

    val props = new Properties()
    props.put("bootstrap.servers", kafkaBootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    val totalMinutes = 120
    val totalSeconds = totalMinutes * 60
    val meanAmbient = 40
    val stdDevAmbient = 5
    val maxCheatDurationMinutes = 2
    val maxCheatDuration = maxCheatDurationMinutes * 60
    val cheatFactor = 0.7

    val fixedDate = LocalDate.of(2024, 5, 1)
    val startTimes = Seq(
      fixedDate.atTime(8, 0),
      fixedDate.atTime(10, 30),
      fixedDate.atTime(14, 0),
      fixedDate.atTime(16, 30)
    )

    val studentsId = (1 to 10).map(i => f"2020000$i%02d")
    val sensorIds = (1 to 10).map(i => f"sensor-$i%02d")

    val baseLatitude = 37.7749
    val baseLongitude = -122.4194

    val random = new Random()
    val latitudes = Seq.fill(10)(baseLatitude + random.nextDouble() * 0.0002 - 0.0001)
    val longitudes = Seq.fill(10)(baseLongitude + random.nextDouble() * 0.0002 - 0.0001)

    val studentData = studentsId.indices.map { i =>
      val studentId = studentsId(i)
      val sensorId = sensorIds(i)
      val latitude = latitudes(i)
      val longitude = longitudes(i)
      val cheatProb = random.nextDouble() * 0.00009 + 0.00001
      val sensorData = NoiseGenerator.generateDailyExamNoiseData(
        StudentData(studentId, sensorId, latitude, longitude),
        startTimes, totalSeconds, meanAmbient, stdDevAmbient, cheatProb, maxCheatDuration, cheatFactor
      )
      (studentId, sensorData)
    }

    val system = ActorSystem("IoTSystem")
    import system.dispatcher

    def scheduleDataSending(studentId: String, data: Seq[SensorData], producer: KafkaProducer[String, String], topic: String): Cancellable = {
      def sendData(second: Int): Unit = {
        if (second < totalSeconds) {
          val sensorData = data(second)
          val report = IoTReport(
            sensorData.timestamp,
            sensorData.sensorId,
            sensorData.moduleId,
            sensorData.studentId,
            sensorData.dB,
            sensorData.latitude,
            sensorData.longitude
          )
          val json = IoTReport.toJson(report)
          producer.send(new ProducerRecord[String, String](topic, json), (metadata, exception) => {
            if (exception != null) {
              println(s"Failed to send data for student $studentId: ${exception.getMessage}")
            }
          })
          println(s"Data for student $studentId sent to Kafka for second ${second + 1}")
          system.scheduler.scheduleOnce(1.second)(sendData(second + 1))
        }
      }

      system.scheduler.scheduleOnce(0.seconds)(sendData(0))
    }

    val cancellables = studentData.map { case (studentId, data) =>
      scheduleDataSending(studentId, data, producer, kafkaTopic)
    }

    system.whenTerminated.onComplete {
      case Success(_) => producer.close()
      case Failure(_) => producer.close()
    }

    Await.result(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  }
}
