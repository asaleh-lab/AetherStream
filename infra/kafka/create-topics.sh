#!/usr/bin/env bash
# Creates the AetherStream Kafka topics. Idempotent: re-running is safe.
# Intended for the docker-compose one-shot init container, but usable manually.
#
#   BOOTSTRAP=localhost:9094 ./create-topics.sh   # from host (after compose up)
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka:9092}"
PARTITIONS="${PARTITIONS:-1}"
REPLICAS="${REPLICAS:-1}"

TOPICS=(
  turbine-events
  grid-events
  energy-state-events
  alerts
  recommendations
  dead-letter-events
  outbox-events
)

echo "Creating topics on ${BOOTSTRAP} (partitions=${PARTITIONS}, replicas=${REPLICAS})"
for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICAS}"
  echo "  ok: ${topic}"
done

echo "Done. Current topics:"
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list
