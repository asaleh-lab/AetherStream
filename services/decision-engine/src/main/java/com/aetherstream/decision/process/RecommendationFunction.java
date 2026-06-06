package com.aetherstream.decision.process;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.Recommendation;
import com.aetherstream.decision.envelope.EventEnvelopeJson;
import com.aetherstream.decision.rules.OptimizationRules;
import java.time.Instant;
import java.util.UUID;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;

/** Consumes energy-state envelopes and emits recommendation envelopes when rules match. */
public class RecommendationFunction extends RichFlatMapFunction<String, String> {

    private final double efficiencyTarget;

    public RecommendationFunction(double efficiencyTarget) {
        this.efficiencyTarget = efficiencyTarget;
    }

    @Override
    public void flatMap(String json, Collector<String> out) {
        var envelope = EventEnvelopeJson.parse(json);
        if (!EventTypes.ENERGY_STATE_COMPUTED.equals(envelope.eventType())) {
            return;
        }

        var state = (EnergyState) envelope.payload();
        OptimizationRules.evaluate(state, efficiencyTarget).ifPresent(suggestion -> {
            var recommendation = new Recommendation(
                    UUID.randomUUID().toString(),
                    state.region(),
                    suggestion,
                    state.timestamp() != null ? state.timestamp() : Instant.now());
            out.collect(EventEnvelopeJson.serializeRecommendation(recommendation, envelope.correlationId()));
        });
    }
}
