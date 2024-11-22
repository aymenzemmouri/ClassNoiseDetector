package speakinganalysis

import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import speakinganalysis.SpeakingAnalysis.Summary
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object Main {
  // Main method to run the program
  def main(args: Array[String]): Unit = {

    // Checking if enough arguments are provided
    if (args.length < 3) {
      print(args.length)
      println("Usage: Main <hdfsPath> <folder> <speakingThreshold>")
      sys.exit(1)
    }

    // Extracting arguments
    val hdfsPath = args(0)
    val folder = args(1)
    val speakingThreshold = args(2).toDouble

    // Constructing HDFS path folder
    val hdfsPathFolder = s"$hdfsPath/$folder"

    // Setting up Spark configuration
    val conf = new SparkConf()
      .setAppName("Analyse de données")
      .setMaster("local[*]")

    // Create a SparkSession
    val spark = SparkSession.builder()
      .config(conf)
      .getOrCreate()

    // Setting HADOOP_USER_NAME system property
    System.setProperty("HADOOP_USER_NAME", "root")

    // Creating Hadoop configuration
    val hadoopConf = new Configuration()

    // Getting the list of Parquet files in the directory
    val parquetFiles = FileSystem.get(new java.net.URI(hdfsPathFolder), hadoopConf)
      .listStatus(new Path(hdfsPathFolder))
      .filter(_.getPath.toString.endsWith(".parquet"))
      .map(_.getPath.toString)

    // Reading all the Parquet files into a DataFrame
    val parquetDF = spark.read.parquet(parquetFiles: _*)

    // Creating an instance of Bot
    val bot = new Bot("https://discord.com/api/webhooks/1240949712090304543/hZQIlqPKLe3bYzpYzXErfUOUtXzP8kYikBZrmHFhu_XiMv-Zr6dQu440TYy93e-pmnRt")

    // Performing speaking analysis
    val allSpeakingDurations = SpeakingAnalysis.identifySpeakingDurations(parquetDF, speakingThreshold)(spark)

    // Calculating summary by module
    val summaryByModule = SpeakingAnalysis.calculateSummaryByModule(allSpeakingDurations)(spark)

    // Calculating summary by student
    val summaryByStudent = SpeakingAnalysis.calculateSummaryByStudent(allSpeakingDurations)(spark)

    // Getting top speaking students
    val topSpeakingStudents = SpeakingAnalysis.getTopSpeakingStudents(allSpeakingDurations, 3)(spark)

    // Formatting all speaking durations as string
    val allSpeakingDurationsString = allSpeakingDurations.collect().map { row =>
      s"""Student ID: ${row.getAs[String]("studentId")}
         |Module ID: ${row.getAs[String]("moduleId")}
         |Total Speaking Duration: ${row.getAs[Double]("speakingDuration")}
         |Total Speaking Count: ${row.getAs[Long]("speakingCount")}
         |""".stripMargin
    }.mkString("\n")

    // Sending the formatted string report
    bot.sendReport(s"📊 All Speaking Durations:\n$allSpeakingDurationsString")

    // Formatting top speaking students as string
    val topSpeakingStudentsString = topSpeakingStudents.collect().map { row =>
      s"Rank: ${row.getAs[Int]("rank")}, " +
        s"Student ID: ${row.getAs[String]("studentId")}, " +
        s"Total Speaking Duration: ${row.getAs[Double]("totalSpeakingDuration")}, "
    }.mkString("\n")

    // Sending the formatted string report
    bot.sendReport(s"📊 Top Speaking Students:\n$topSpeakingStudentsString")

    // Formatting summary by module as string
    val summaryByModuleMessage =
      s"""📊 Summary by Module:
         |${
        summaryByModule.map { case (moduleId, summary) =>
          s"Module ID: $moduleId, Total Speaking Duration: ${summary.totalSpeakingDuration}, Total Speaking Count: ${summary.totalSpeakingCount}"
        }.mkString("\n")
      }""".stripMargin

    // Sending the summary by module report
    bot.sendReport(summaryByModuleMessage)

    // Formatting summary by student as string
    val summaryByStudentMessage =
      s"""📊 Summary by Student:
         |${
        summaryByStudent.map { case (studentId, summary) =>
          s"Student ID: $studentId, Total Speaking Duration: ${summary.totalSpeakingDuration}, Total Speaking Count: ${summary.totalSpeakingCount}"
        }.mkString("\n")
      }""".stripMargin

    // Sending the summary by student report
    bot.sendReport(summaryByStudentMessage)

    // Creating a directory with the date
    val outputDir = s"data/$folder"
    Files.createDirectories(Paths.get(outputDir))

    // Exporting the results as JSON to the created directory
    val allSpeakingDurationsJson = writeJsonToFile(allSpeakingDurations, s"$outputDir/all_speaking_durations.json")
    bot.sendFile("All Speaking Durations",s"$outputDir/all_speaking_durations.json")

    val summaryByModuleJson = writeJsonToFile(summaryByModule, s"$outputDir/summary_by_module.json")
    bot.sendFile("Summary by Module", s"$outputDir/summary_by_module.json")

    val summaryByStudentJson = writeJsonToFile(summaryByStudent, s"$outputDir/summary_by_student.json")
    bot.sendFile("Summary by Student", s"$outputDir/summary_by_student.json")

    val topSpeakingStudentsJson = writeJsonToFile(topSpeakingStudents, s"$outputDir/top_speaking_students.json")
    bot.sendFile("Top Speaking Students", s"$outputDir/top_speaking_students.json")

    // Stopping the Spark session
    spark.stop()
  }

  // Method to write data as JSON to file
  def writeJsonToFile(data: Any, filePath: String): Unit = {
    val jsonString = data match {
      case df: DataFrame => df.toJSON.collect().mkString("[", ",", "]")
      case summaryMap: Map[String, Summary] => summaryMap.asJson.spaces2
      case _ => throw new IllegalArgumentException("Type de données non pris en charge")
    }
    Files.write(Paths.get(filePath), jsonString.getBytes(StandardCharsets.UTF_8))
  }
}