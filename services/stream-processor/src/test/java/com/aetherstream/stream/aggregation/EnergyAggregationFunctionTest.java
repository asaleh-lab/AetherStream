package com.aetherstream.stream.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.stream.envelope.EventEnvelopeJson;
import com.aetherstream.stream.model.StreamEvent;
import java.time.Instant;
import java.util.List;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnergyAggregationFunctionTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");
    private static final String CORRELATION = "corr-agg-001";

    private KeyedOneInputStreamOperatorTestHarness<String, StreamEvent, String> harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new KeyedOneInputStreamOperatorTestHarness<>(
                new KeyedProcessOperator<>(new EnergyAggregationFunction()),
                StreamEvent::region,
                Types.STRING);
        harness.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        harness.close();
    }

    @Test
    void emitsEnergyStateWhenGridAndTurbineDataPresent() throws Exception {
        var grid = new GridLoad("north-sea", 2.0, 2.5, NOW);
        harness.processElement(StreamEvent.grid(gridEnvelope(grid), grid), NOW.toEpochMilli());

        var turbine = new Turbine("T-001", 10, 1500, 0.2, NOW);
        harness.processElement(StreamEvent.turbine(turbineEnvelope(turbine), turbine, "north-sea"), NOW.toEpochMilli());

        List<String> output = harness.extractOutputValues();
        assertThat(output).hasSize(1);
        assertThat(output.getFirst()).contains(EventTypes.ENERGY_STATE_COMPUTED);
        assertThat(output.getFirst()).contains("north-sea");
        assertThat(output.getFirst()).contains("1500.0");
        assertThat(output.getFirst()).contains(CORRELATION);
    }

    @Test
    void computeEfficiency_clampsToOne() {
        assertThat(EnergyAggregationFunction.computeEfficiency(5000, 2.0)).isEqualTo(1.0);
    }

    private static com.aetherstream.domain.event.EventEnvelope gridEnvelope(GridLoad grid) {
        return new com.aetherstream.domain.event.EventEnvelope(
                java.util.UUID.randomUUID(),
                EventTypes.GRID_LOAD_RECORDED,
                NOW,
                CORRELATION,
                grid);
    }

    private static com.aetherstream.domain.event.EventEnvelope turbineEnvelope(Turbine turbine) {
        return new com.aetherstream.domain.event.EventEnvelope(
                java.util.UUID.randomUUID(),
                EventTypes.TURBINE_TELEMETRY_RECORDED,
                NOW,
                CORRELATION,
                turbine);
    }
}
