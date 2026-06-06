#!/bin/sh
set -eu

PROMETHEUS_URL="${PROMETHEUS_URL:-http://prometheus:9090}"
sed -i "s|\${PROMETHEUS_URL}|${PROMETHEUS_URL}|g" /etc/grafana/provisioning/datasources/azure.yaml

exec /run.sh
