import java.sql.Timestamp
import scala.util.{Random, Try}

import upickle.default._

import java.sql.Timestamp
import scala.util.Try

case class IoTReport(
                      timestamp: Timestamp,
                      sensorId: String,
                      moduleId: String,
                      studentId: String,
                      dB: Double,
                      latitude: Double,
                      longitude: Double
                    )

object IoTReport {

  implicit val timestampRw: ReadWriter[Timestamp] = readwriter[String].bimap[Timestamp](
    timestamp => timestamp.toString,
    str => Timestamp.valueOf(str)
  )

  implicit val rw: ReadWriter[IoTReport] = macroRW[IoTReport]

  def toJson(report: IoTReport): String = write(report)

  def fromJson(json: String): Option[IoTReport] = {
    Try(Some(read[IoTReport](json))).getOrElse(None)
  }


  def toCsv(report: IoTReport): String = {
    s"${report.timestamp},${report.sensorId},${report.moduleId},${report.studentId},${report.dB},${report.latitude},${report.longitude}"
  }

  def fromCsv(csv: String): Option[IoTReport] = {
    val fields = csv.split(",")
    if (fields.length == 7) {
      Try {
        val timestamp = Timestamp.valueOf(fields(0))
        val sensorId = fields(1)
        val moduleId = fields(2)
        val studentId = fields(3)
        val dB = fields(4).toDouble
        val latitude = fields(5).toDouble
        val longitude = fields(6).toDouble
        IoTReport(timestamp, sensorId, moduleId, studentId, dB, latitude, longitude)
      }.toOption
    } else {
      None
    }
  }
}
