
# IoT Sensor Data Simulator for Exam Monitoring

This project simulates fake exam noise from sensors linked to students and sends JSON data to a Kafka stream each second. The purpose of this project is to generate and stream realistic noise data for students during exams to a Kafka topic.

## Usage

The application will start generating fake exam noise data and send it to the specified Kafka topic every second. You can monitor the Kafka topic to see the incoming data.

### Steps to Launch

1. **Navigate to the project directory**:

    ```bash
    cd path/to/Kafka IoT Project - Data Ing
    ```

2. **Run the application**:

    ```bash
    sbt run
    ```

## Project Structure

```plaintext
Kafka IoT Project - Data Ing/
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── models/
│   │   │   │   ├── SensorData.scala
│   │   │   │   └── StudentData.scala
│   │   │   ├── Main.scala
│   │   │   ├── NoiseGenerator.scala
│   │   │   └── IoTReport.scala
│   └── test/
│       └── scala/
├── build.sbt
├── README.md
```

### Main Components

- **Main.scala**: Entry point of the application. Configures Kafka producer and schedules data generation.
- **NoiseGenerator.scala**: Generates realistic noise data simulating student cheating scenarios.
- **IoTReport.scala**: Defines the IoT report structure and handles JSON serialization/deserialization.
