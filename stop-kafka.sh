#!/bin/bash

# Kafka stop script

# Step 1: Delete the Kafka topic 'sensor-readings'
echo "Deleting Kafka topic 'sensor-readings'..."
kafka-topics.sh --delete --topic sensor-readings --bootstrap-server localhost:9092

# Ensure the topic has been deleted
sleep 10

# Step 2: Stop the Kafka server
echo "Stopping Kafka server..."
kafka-server-stop.sh

# Ensure the Kafka server has stopped completely
sleep 15

# Step 3: Remove Kafka logs
echo "Removing Kafka logs..."
rm -r /private/tmp/kraft-combined-logs

# Confirmation message
echo "Kafka server stopped, topic deleted, and logs removed."
