package com.aetherstream.stream.anomaly;

import com.aetherstream.domain.model.Alert;
import com.aetherstream.stream.envelope.EventEnvelopeJson;
import com.aetherstream.stream.model.StreamEvent;
import com.aetherstream.stream.model.StreamEventKind;
import java.util.List;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

/** Evaluates keyed ingest events and emits alert envelopes for detected anomalies. */
public class AnomalyDetectionFunction implements FlatMapFunction<StreamEvent, String> {

    private final double vibrationThreshold;

    public AnomalyDetectionFunction(double vibrationThreshold) {
        this.vibrationThreshold = vibrationThreshold;
    }

    @Override
    public void flatMap(StreamEvent event, Collector<String> out) {
        List<Alert> alerts =
                switch (event.kind()) {
                    case TURBINE -> AnomalyRules.evaluateTurbine(
                            event.turbine(), vibrationThreshold, event.eventTime());
                    case GRID -> AnomalyRules.evaluateGrid(event.grid(), event.eventTime());
                    case WEATHER -> List.of();
                };

        for (Alert alert : alerts) {
            out.collect(EventEnvelopeJson.serializeAlert(alert, event.correlationId()));
        }
    }
}
