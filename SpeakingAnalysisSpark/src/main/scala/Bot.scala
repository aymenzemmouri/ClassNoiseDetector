package speakinganalysis

import play.api.libs.json.Json
import sttp.client3._
import sttp.model.MediaType

import java.io.File
import java.nio.charset.StandardCharsets

// Class definition for Bot
class Bot(url: String) {
  // Setting up headers for HTTP requests
  val headers: Map[String, String] = Map(
    "Content-Type" -> "application/json"
  )

  // Implicitly defining backend for HTTP requests
  implicit val backend = HttpURLConnectionBackend()

  // Method to send a report message
  def sendReport(message: String): Int = {
    // Creating a map with message content
    val map = Map(
      "content" -> message
    )
    // Converting map to JSON string
    val data = Json.stringify(Json.toJson(map))

    // Building HTTP request
    val request = basicRequest
      .post(uri"$url")
      .headers(headers)
      .body(data)

    // Sending the request and returning response code
    val response = request.send()
    response.code.code
  }

  // Implicitly defining file body serializer for multipart requests
  implicit val fileBodySerializer: BodySerializer[File] = { file =>
    StringBody(file.getAbsolutePath, StandardCharsets.UTF_8.toString, MediaType.ApplicationJson)
  }

  // Method to send a file along with description
  def sendFile(description: String, filePath: String): Int = {
    // Creating a File object from file path
    val file = new File(filePath)

    // Building HTTP request for sending file
    val request = basicRequest
      .post(uri"$url")
      .headers(headers)
      .multipartBody(
        multipart("file", file)
          .fileName(file.getName)
          .contentType(MediaType.ApplicationJson), // Setting file content type
        multipart("description", description) // Adding file description as multipart
      )

    // Sending the request and returning response code
    val response = request.send()
    response.code.code
  }

}
