package com.aetherstream.stream.ingest;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.stream.envelope.EventEnvelopeJson;
import com.aetherstream.stream.model.StreamEvent;
import com.aetherstream.stream.model.TurbineRegionMapper;
import org.apache.flink.api.common.functions.MapFunction;

/** Maps raw Kafka JSON envelopes to normalized {@link StreamEvent} records. */
public class IngestEventMapper implements MapFunction<String, StreamEvent> {

    @Override
    public StreamEvent map(String json) {
        EventEnvelope envelope = EventEnvelopeJson.parse(json);
        return switch (envelope.eventType()) {
            case EventTypes.TURBINE_TELEMETRY_RECORDED -> {
                Turbine turbine = (Turbine) envelope.payload();
                String region = TurbineRegionMapper.regionFor(turbine.turbineId());
                yield StreamEvent.turbine(envelope, turbine, region);
            }
            case EventTypes.GRID_LOAD_RECORDED -> {
                GridLoad gridLoad = (GridLoad) envelope.payload();
                yield StreamEvent.grid(envelope, gridLoad);
            }
            default -> throw new IllegalArgumentException("Unsupported ingest event type: " + envelope.eventType());
        };
    }
}
