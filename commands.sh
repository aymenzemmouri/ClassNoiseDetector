hdfs namenode -format

start-dfs.sh
start-yarn.sh
hdfs dfs -mkdir hdfs://localhost:9000/reports

hdfs dfs -ls /

./kafka-server-start.sh 

java -jar KafkaDataGenerator.jar sensor-readings localhost:9092

kafka-console-consumer.sh --topic sensor-readings --from-beginning --bootstrap-server localhost:9092

java -jar StreamProcessingFlinkEngine.jar sensor-readings localhost:9092 50 "https://discord.com/api/webhooks/1240949326130184233/-xuBMqLD29xBkCbP28KC8OTln-7DVdt19lAhh9mD6lLgaGoERTDRSG7q3_N5RkQKedRA"

spark-submit \
  --class Main \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.1.2 \
  --conf spark.sql.streaming.checkpointLocation=hdfs://localhost:9000/checkpoints \
  ./BatchProcessingSparkStreamEngine.jar \
  sensor-readings localhost:9092 "10 seconds" month hdfs://localhost:9000/reports

hdfs dfs -ls /reports

java -jar SpeakingAnalysisSpark.jar hdfs://localhost:9000/reports/ 21-05-2024 45.0
