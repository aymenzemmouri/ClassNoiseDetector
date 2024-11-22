package speakinganalysis

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object SpeakingAnalysis {

  case class Summary(
                      totalSpeakingDuration: Double,
                      totalSpeakingCount: Double
                    )

  def identifySpeakingDurations(data: DataFrame, speakingThreshold: Double)(implicit spark: SparkSession): DataFrame = {
    import spark.implicits._

    // Filtrer les données où le volume de la parole est supérieur au seuil
    val speakingData = data.filter($"dB" > speakingThreshold)

    // Créer une fenêtre de partition par étudiant et module, ordonnée par timestamp
    val windowSpec = Window.partitionBy("studentId", "moduleId").orderBy("timestamp")

    // Ajouter une colonne "nextTimestamp" qui contient le timestamp suivant pour chaque enregistrement
    val speakingDataWithNextTimestamp = speakingData.withColumn("nextTimestamp", lead($"timestamp", 1).over(windowSpec))

    // Ajouter une colonne "isConsecutiveSpeaking" pour marquer les enregistrements où les timestamp sont consécutifs
    val speakingDataWithConsecutiveSpeaking = speakingDataWithNextTimestamp.withColumn("isConsecutiveSpeaking",
      when($"nextTimestamp".isNull, false)
        .otherwise(unix_timestamp($"nextTimestamp") - unix_timestamp($"timestamp") === lit(1)))

    // Compter le nombre total d'événements de parole distincts pour chaque étudiant dans chaque module
    val speakingDurations = speakingDataWithConsecutiveSpeaking
      .groupBy($"studentId", $"moduleId")
      .agg(
        (count($"timestamp") * lit(1.0)).as("speakingDuration"), // Convertir le résultat en Double
        (countDistinct(when($"isConsecutiveSpeaking", $"timestamp")) + lit(1)).as("speakingCount") // Pour compter les événements de parole distincts
      )

    speakingDurations
  }

  def calculateSummaryByModule(allSpeakingDurations: DataFrame)(implicit spark: SparkSession): Map[String, Summary] = {
    import spark.implicits._

    val summary = allSpeakingDurations
      .groupBy("moduleId")
      .agg(
        sum("speakingDuration").as("totalSpeakingDuration"),
        count("speakingDuration").as("totalSpeakingCount")
      )
      .select($"moduleId", $"totalSpeakingDuration", $"totalSpeakingCount")

    summary.collect().map { row =>
      val moduleId = row.getString(0)
      val totalSpeakingDuration = row.getDouble(1)
      val totalSpeakingCount = row.getLong(2).toDouble

      moduleId -> Summary(
        totalSpeakingDuration,
        totalSpeakingCount)
    }.toMap
  }

  def calculateSummaryByStudent(allSpeakingDurations: DataFrame)(implicit spark: SparkSession): Map[String, Summary] = {
    import spark.implicits._

    // Agréger les données par studentId pour calculer la durée totale et le nombre total de prises de parole
    val summary = allSpeakingDurations
      .groupBy("studentId")
      .agg(
        sum("speakingDuration").as("totalSpeakingDuration"),
        count("*").as("totalSpeakingCount") // Utilise count(*) pour compter le nombre de lignes
      )
      .select($"studentId", $"totalSpeakingDuration", $"totalSpeakingCount")

    // Convertir les résultats agrégés en une Map de studentId à Summary
    summary.collect().map { row =>
      val studentId = row.getString(0)
      val totalSpeakingDuration = row.getDouble(1)
      val totalSpeakingCount = row.getLong(2)

      studentId -> Summary(
        totalSpeakingDuration,
        totalSpeakingCount
      )
    }.toMap
  }

  def getTopSpeakingStudents(allSpeakingDurations: DataFrame, topN: Int)(implicit spark: SparkSession): DataFrame = {
    import spark.implicits._

    // Calcule la somme de la durée de parole pour chaque étudiant
    val speakingDurationByStudent = allSpeakingDurations
      .groupBy("studentId")
      .agg(sum("speakingDuration").as("totalSpeakingDuration"))

    // Crée une fenêtre pour classer les étudiants par ordre décroissant de durée de parole
    val windowSpec = Window.orderBy($"totalSpeakingDuration".desc)

    // Attribue un rang à chaque étudiant en fonction de sa durée de parole
    val rankedStudents = speakingDurationByStudent
      .withColumn("rank", rank().over(windowSpec))
      .filter($"rank" <= topN) // Ne conserve que les top N étudiants

    rankedStudents
  }

}
