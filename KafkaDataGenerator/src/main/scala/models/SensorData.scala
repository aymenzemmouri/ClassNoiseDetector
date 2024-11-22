// src/main/scala/models/SensorData.scala

package models

import java.sql.Timestamp

case class SensorData(
  timestamp: Timestamp,
  sensorId: String,
  moduleId: String,
  studentId: String,
  dB: Double,
  latitude: Double,
  longitude: Double
)
