#!/bin/bash

# Default Kafka directory
KAFKA_DIR="/opt/kafka_2.13-3.7.0"  # Remplacez par le chemin réel de votre installation Kafka
TOPIC_NAME="sensor-readings"

# Générer un UUID aléatoire pour l'ID du cluster Kafka
KAFKA_CLUSTER_ID="$(kafka-storage.sh random-uuid)"

# Formater le stockage pour Kafka en utilisant l'ID du cluster généré et le fichier de configuration spécifié
echo "Formatting Kafka storage with cluster ID: $KAFKA_CLUSTER_ID"
kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c $KAFKA_DIR/config/kraft/server.properties

# Démarrer le serveur Kafka avec le fichier de configuration spécifié
echo "Starting Kafka server..."
kafka-server-start.sh $KAFKA_DIR/config/kraft/server.properties &

# Attendre quelques secondes pour s'assurer que le serveur Kafka est en cours d'exécution
sleep 10

# Vérifier si le sujet existe déjà
EXISTING_TOPIC=$(kafka-topics.sh --list --bootstrap-server localhost:9092 | grep "^$TOPIC_NAME$")

if [ -z "$EXISTING_TOPIC" ]; then
  # Créer un sujet Kafka avec le nom spécifié
  echo "Creating Kafka topic: $TOPIC_NAME"
  kafka-topics.sh --create --topic $TOPIC_NAME --bootstrap-server localhost:9092
else
  echo "Kafka topic '$TOPIC_NAME' already exists."
fi

echo "Kafka setup complete."
