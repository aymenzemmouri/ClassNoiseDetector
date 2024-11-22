
# SpeakingAnalysis

## Overview
SpeakingAnalysis is a Scala-based project designed to analyze speaking durations and events from CSV data. The project reads CSV files containing speaking data, processes the information to calculate various statistics, and outputs the results in JSON format.

## Features
- Read and process CSV files for multiple modules
- Identify speaking durations based on a specified decibel threshold
- Calculate summary statistics for each module
- Determine speaking counts per module and total speaking counts per student
- Identify top students based on speaking counts
- Export results to a JSON file

## Prerequisites
- Scala 2.13.12
- SBT (Scala Build Tool)

## Getting Started

### Build the project
```bash
sbt compile
```

### Run the project
```bash
sbt run
```

## Project Structure
- `src/main/scala/Main.scala`: Entry point of the application
- `src/main/scala/SpeakingAnalysis.scala`: Contains methods for processing speaking data and calculating statistics
- `src/main/scala/utils/JsonUtils.scala`: Utility for JSON serialization
- `src/main/scala/CSVFileReader.scala`: Utility for reading CSV files

## Configuration
- Modify the `date`, `durationThreshold`, `speakingThreshold`, and `numModules` variables in `Main.scala` to fit your data and analysis needs.

## Dependencies
The project relies on the following libraries:
- Jackson Databind and Scala Module for JSON processing
- Tototoshi CSV for reading CSV files

These dependencies are managed by SBT and are specified in the `build.sbt` file.