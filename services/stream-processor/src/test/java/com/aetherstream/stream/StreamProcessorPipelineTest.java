package com.aetherstream.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.AlertType;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.stream.anomaly.AnomalyDetectionFunction;
import com.aetherstream.stream.envelope.EventEnvelopeJson;
import com.aetherstream.stream.ingest.IngestEventMapper;
import com.aetherstream.stream.model.StreamEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.Test;

/** Verifies ingest mapping, aggregation, and anomaly branches without a live Kafka/Flink cluster. */
class StreamProcessorPipelineTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");
    private static final String CORRELATION = "corr-pipeline-001";

    @Test
    void ingestToAggregation_emitsEnergyState() throws Exception {
        var mapper = new IngestEventMapper();
        StreamEvent gridEvent = mapper.map(gridJson());
        StreamEvent turbineEvent = mapper.map(turbineJson());

        KeyedOneInputStreamOperatorTestHarness<String, StreamEvent, String> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        new KeyedProcessOperator<>(new com.aetherstream.stream.aggregation.EnergyAggregationFunction()),
                        StreamEvent::region,
                        Types.STRING);
        harness.open();
        harness.processElement(gridEvent, NOW.toEpochMilli());
        harness.processElement(turbineEvent, NOW.toEpochMilli());

        assertThat(harness.extractOutputValues()).singleElement().satisfies(json -> {
            assertThat(json).contains(EventTypes.ENERGY_STATE_COMPUTED);
            assertThat(json).contains("north-sea");
            assertThat(json).contains(CORRELATION);
        });
        harness.close();
    }

    @Test
    void ingestToAnomaly_emitsVibrationAlert() throws Exception {
        var mapper = new IngestEventMapper();
        StreamEvent turbineEvent = mapper.map(highVibrationTurbineJson());

        var anomaly = new AnomalyDetectionFunction(1.0);
        List<String> alerts = new ArrayList<>();
        anomaly.flatMap(turbineEvent, new Collector<String>() {
            @Override
            public void collect(String record) {
                alerts.add(record);
            }

            @Override
            public void close() {}
        });

        assertThat(alerts).anyMatch(json -> json.contains(AlertType.VIBRATION_SPIKE.name()));
    }

    private static String gridJson() {
        var grid = new GridLoad("north-sea", 2.0, 2.5, NOW);
        var envelope = new com.aetherstream.domain.event.EventEnvelope(
                UUID.randomUUID(), EventTypes.GRID_LOAD_RECORDED, NOW, CORRELATION, grid);
        return EventEnvelopeJson.serialize(envelope);
    }

    private static String turbineJson() {
        var turbine = new Turbine("T-001", 10, 1500, 0.2, NOW);
        var envelope = new com.aetherstream.domain.event.EventEnvelope(
                UUID.randomUUID(), EventTypes.TURBINE_TELEMETRY_RECORDED, NOW, CORRELATION, turbine);
        return EventEnvelopeJson.serialize(envelope);
    }

    private static String highVibrationTurbineJson() {
        var turbine = new Turbine("T-001", 10, 1500, 1.5, NOW);
        var envelope = new com.aetherstream.domain.event.EventEnvelope(
                UUID.randomUUID(), EventTypes.TURBINE_TELEMETRY_RECORDED, NOW, CORRELATION, turbine);
        return EventEnvelopeJson.serialize(envelope);
    }
}
