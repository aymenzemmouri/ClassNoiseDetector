// NoiseGenerator.scala

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.util.Random
import models.{StudentData, SensorData}

// Object responsible for generating noise data
object NoiseGenerator {

  // Function to generate noise data for a student's exam
  def generateStudentExamNoiseData(
    numSamples: Int, mean: Double, stdDev: Double, cheatProb: Double,
    maxCheatDuration: Int, cheatFactor: Double
  ): Seq[Double] = {
    val random = new Random()

    // Recursive function to generate noise data
    def generateNoise(remainingSamples: Int, cheating: Boolean, cheatDuration: Int, acc: Seq[Double]): Seq[Double] = {
      if (remainingSamples == 0) acc
      else {
        val noise = random.nextGaussian() * stdDev + mean
        val (newCheating, newCheatDuration, adjustedNoise) = if (!cheating && random.nextDouble() < cheatProb) {
          (true, random.nextInt(maxCheatDuration + 1), noise * cheatFactor)
        } else if (cheating) {
          (cheating, cheatDuration - 1, noise * cheatFactor)
        } else {
          (cheating, cheatDuration, noise)
        }

        val nextCheating = if (newCheatDuration <= 0) false else newCheating
        generateNoise(remainingSamples - 1, nextCheating, newCheatDuration, acc :+ adjustedNoise)
      }
    }

    generateNoise(numSamples, false, 0, Seq.empty[Double])
  }

  // Function to generate daily noise data for exams
  def generateDailyExamNoiseData(
    studentData: StudentData, startTimes: Seq[LocalDateTime], totalSeconds: Int,
    mean: Double, stdDev: Double, cheatProb: Double, maxCheatDuration: Int,
    cheatFactor: Double
  ): Seq[SensorData] = {
    startTimes.zipWithIndex.flatMap { case (startTime, i) =>
      val noiseData = generateStudentExamNoiseData(totalSeconds, mean, stdDev, cheatProb, maxCheatDuration, cheatFactor)
      val timestamps = (0 until totalSeconds).map(startTime.plusSeconds(_)).map(ts => Timestamp.valueOf(ts))
      val moduleIds = (0 until totalSeconds).map(_ => s"module-${i + 1}")

      (timestamps zip noiseData zip moduleIds).map {
        case ((timestamp, dB), moduleId) =>
          SensorData(timestamp, studentData.sensorId, moduleId, studentData.studentId, dB, studentData.latitude, studentData.longitude)
      }
    }
  }
}