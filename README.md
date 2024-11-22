# **Sensor Data Processing Pipeline**

This project implements a robust and scalable data processing pipeline to handle sensor readings from classrooms. The system ingests real-time data from a Kafka topic, processes it using Apache Flink and Spark, and stores the results in HDFS. It also provides an analysis component for generating actionable insights and reports.

## **Components Overview**

### **1. Kafka Data Generator**
Simulates real-time sensor data and sends it to a Kafka topic. Configurable intervals ensure realistic sensor readings for seamless downstream integration.

### **2. Flink Stream Processing Engine**
Analyzes sensor data in real-time, detects anomalies, and sends alerts. Leverages Flink's scalability and fault-tolerant features for instant insights.

### **3. Spark Batch Processing Engine**
Processes historical sensor data in batches, aggregates it for trends, and stores results in HDFS. Ideal for periodic analysis and reporting.

### **4. Spark Analysis Engine**
Generates insights from historical data stored in HDFS, identifying patterns and trends. Outputs detailed, configurable reports for actionable decisions.

## **Features**

- **Real-Time Data Processing:** Leveraging Apache Flink for immediate analysis and alerting.  
- **Batch Processing:** Using Apache Spark for historical data aggregation and insights.  
- **Scalable Architecture:** Distributed systems like Kafka, Flink, and Spark ensure fault tolerance and high throughput.  
- **Actionable Insights:** Alerts and comprehensive reports for excessive talking in classrooms.  

## **Prerequisites**

Before setting up the project, ensure the following dependencies are installed:

- **Apache Hadoop:** 3.3.6 (HDFS and YARN)  
- **Apache Kafka:** 3.7.0  
- **Apache Spark:** 3.5.1  
- **Apache Flink:** 1.19  
- **Java:** 11  
- **Scala:** 2.12.17  
- **sbt:** 1.8.0  

> **Note:** Ensure Kafka is added to your system's environment variables. Add the following lines:  

```bash
export KAFKA_DIR=/opt/kafka_2.13-3.7.0
export PATH=$PATH:$KAFKA_DIR/bin
```

## Prerequisites

- Apache Hadoop 3.3.6 (HDFS and YARN)
- Apache Kafka 3.7.0 
- Apache Spark 3.5.1
- Apache Flink 1.19
- Java 11
- Scala 2.12.17
- sbt.version=1.8.0

**Note :** Ensure that Kafka is added to your system's environment variables.

Add the following lines to your environment variables:

```sh
# export KAFKA_DIR=/path/of/kafka
export /opt/kafka_2.13-3.7.0
export PATH=$PATH:$KAFKA_DIR/bin
```


## Build

Clone the project repository using Git:

```bash
git clone <repository-url>
cd <project-directory>
```

### Using Command Line

1. **Install sbt (Scala Build Tool)**  
If you don't have sbt installed, you can download and install it from the official [sbt website](https://www.scala-sbt.org/).

2. **Build JAR Files**
Run the following command to build the project and generate the JAR files:
```bash
sbt clean assembly
```

### Using IntelliJ

To build the project from source using an IDE (IntelliJ), follow these steps:

- Open each project in IntelliJ IDEA
- Generate JAR files for each component (see instructions in the project)
- Note: Make sure to specify the main class in the manifest file when generating JAR files.

1. Open your Scala project in IntelliJ IDEA.

2. Go to `File > Project Structure` (or press `Ctrl+Alt+Shift+S` on Windows/Linux, `Cmd+;` on macOS).

3. In the Project Structure window, select `Artifacts` from the left pane.

4. Click the `+` icon and select `JAR > From modules with dependencies`.

5. In the "Create JAR from Modules" window, select the main module of your project and click `OK`.

6. In the "Extract Configuration to .xml" window, choose the location to save the artifact configuration file and click `OK`.

7. Back in the Project Structure window, select the newly created artifact in the right pane.

8. Click the `Output Layout` tab and ensure that the `Main Class` field is set to the fully qualified name of your application's entry point (e.g., `com.example.MyApp`).

9. Click `OK` to close the Project Structure window.

10. From the main menu, go to `Build > Build Artifacts` (or press `Ctrl+F9` on Windows/Linux, `Cmd+F9` on macOS).

11. In the "Build Artifacts" window, select the artifact you created and click `Build`.

After the build process completes successfully, you should find the generated JAR file in the `out/artifacts` directory within your project folder. You can then run the JAR file using the `java -jar` command.

If you encounter error when lucnhing the jar : Could not find or load main class, execute this : 

```sh
zip -d <jar-name>.jar 'META-INF/*.SF' 'META-INF/*.DSA'
```

## Setup

1. Start HDFS and YARN

```bash
# Format the namenode
hdfs namenode -format

# Start HDFS and YARN daemons
start-dfs.sh
start-yarn.sh
# Alternatively, use start-all.sh
```

2. Create the reports folder in HDFS

```bash
# hdfs dfs -mkdir <HDFS_PATH>
hdfs dfs -mkdir hdfs://localhost:9000/reports
```

3. Start Kafka Server
```bash
# Start the Kafka server with parameters
# ./start-kafka.sh
./start-kafka.sh
```

4. Start Kafka Data Generator
```bash
# Generate sensor data and send it to the Kafka topic
# java -jar <kafka-topic> <kafka-bootstrap-servers>
java -jar KafkaDataGenerator.jar sensor-readings localhost:9092
```

5. Start Flink Stream Processing Engine
```bash
# Process sensor data stream and send alerts
# java -jar <kafka-topic> <kafka-bootstrap-servers> <processing-interval> <alert-threshold> <url-alerting>
zip -d StreamProcessingFlinkEngine.jar 'META-INF/*.SF' 'META-INF/*.DSA'
java -jar StreamProcessingFlinkEngine.jar sensor-readings localhost:9092 50 "https://discord.com/api/webhooks/1240949326130184233/-xuBMqLD29xBkCbP28KC8OTln-7DVdt19lAhh9mD6lLgaGoERTDRSG7q3_N5RkQKedRA"
```

6. Start Spark Batch Processing Engine
```bash
# Process sensor data in batches and store results in HDFS
# java -jar <kafka-topic> <kafka-bootstrap-servers> <processing-interval> <storage-mode> <hdfs-base-path>

spark-submit \
  --class Main \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.1.2 \
  --conf spark.sql.streaming.checkpointLocation=hdfs://localhost:9000/checkpoints \
  ./BatchProcessingSparkStreamEngine.jar \
  sensor-readings localhost:9092 "10 seconds" month hdfs://localhost:9000/reports
```


7. Start Spark Analysis Engine
```bash
# Analyze sensor data in HDFS
# java -jar <hdfsPath> <folder> <speakingThreshold>
java -jar SpeakingAnalysisSpark.jar hdfs://localhost:9000/reports/ 21-05-2024 45.0
```
