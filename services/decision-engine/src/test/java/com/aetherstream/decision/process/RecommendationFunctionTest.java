package com.aetherstream.decision.process;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.decision.envelope.EventEnvelopeJson;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.Test;

class RecommendationFunctionTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Test
    void emitsRecommendationForLowEfficiencyEnergyState() throws Exception {
        var state = new EnergyState("north-sea", 2000, 4.0, 0.5, NOW);
        String input = energyStateJson(state);

        var function = new RecommendationFunction(0.85);
        var collector = new TestCollector();
        function.flatMap(input, collector);

        assertThat(collector.values).hasSize(1);
        assertThat(collector.values.getFirst()).contains(EventTypes.RECOMMENDATION_ISSUED);
        assertThat(collector.values.getFirst()).contains("north-sea");
    }

    @Test
    void ignoresNonEnergyStateEvents() throws Exception {
        var function = new RecommendationFunction(0.85);
        var collector = new TestCollector();
        function.flatMap(
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","correlationId":"corr",\
                "payload":{"region":"north-sea","totalWindPower":1000,"gridDemand":2.0,\
                "efficiencyScore":0.5,"timestamp":"%s"}}
                """
                        .formatted(UUID.randomUUID(), EventTypes.ALERT_RAISED, NOW, NOW),
                collector);

        assertThat(collector.values).isEmpty();
    }

    private static String energyStateJson(EnergyState state) {
        var envelope = new com.aetherstream.domain.event.EventEnvelope(
                UUID.randomUUID(),
                EventTypes.ENERGY_STATE_COMPUTED,
                state.timestamp(),
                "corr-test",
                state);
        return EventEnvelopeJson.serialize(envelope);
    }

    private static final class TestCollector implements Collector<String> {
        private final List<String> values = new java.util.ArrayList<>();

        @Override
        public void collect(String record) {
            values.add(record);
        }

        @Override
        public void close() {}
    }
}
