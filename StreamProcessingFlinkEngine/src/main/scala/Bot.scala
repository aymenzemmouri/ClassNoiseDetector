import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3.{HttpURLConnectionBackend, _}

case class Alert(content: String)
case class Report(content: String)

class Bot(url: String) {
  val urlAlerting: String = url
  val headers: Map[String, String] = Map(
    "Content-Type" -> "application/json"
  )

  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  def sendAlert(timestamp: String, studentId: String, moduleId: String, lat: Double, long: Double, decibel: Double): Int = {
    val message = s"""🚨 Alert : A student is disturbing a session.
                     |moduleId: $moduleId,
                     |studentId: $studentId,
                     |decibel: $decibel
                     |Location : $lat, $long
                     |""".stripMargin

    val map = Map(
      "content" -> message)
    val data = map.asJson.noSpaces
    val request = basicRequest
      .post(uri"$urlAlerting")
      .headers(headers)
      .body(data)

    val response = request.send()
    response.code.code
  }

}
