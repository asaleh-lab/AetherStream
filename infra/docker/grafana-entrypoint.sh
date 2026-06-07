#!/bin/sh
set -eu

LOKI_URL="${LOKI_URL:-http://loki:3100}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://prometheus:9090}"
PROV_DIR="/tmp/grafana-provisioning"

rm -rf "$PROV_DIR"
mkdir -p "$PROV_DIR/datasources" "$PROV_DIR/dashboards/json"
cp -R /etc/grafana/provisioning/dashboards/. "$PROV_DIR/dashboards/" 2>/dev/null || true
sed -e "s|\${LOKI_URL}|${LOKI_URL}|g" \
    -e "s|\${PROMETHEUS_URL}|${PROMETHEUS_URL}|g" \
    /etc/grafana/provisioning/datasources/azure.yaml > "$PROV_DIR/datasources/azure.yaml"

export GF_PATHS_PROVISIONING="$PROV_DIR"
exec /run.sh
