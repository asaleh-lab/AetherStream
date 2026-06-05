package com.aetherstream.stream.aggregation;

import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.stream.envelope.EventEnvelopeJson;
import com.aetherstream.stream.model.StreamEvent;
import java.time.Instant;
import java.util.Map;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * Keyed-by-region aggregation using Flink state: sums turbine power, tracks latest grid load,
 * and emits {@code energy-state-events} when enough partners are present.
 */
public class EnergyAggregationFunction extends KeyedProcessFunction<String, StreamEvent, String> {

    private transient MapState<String, Double> turbinePowerKw;
    private transient ValueState<Double> gridDemandMw;
    private transient ValueState<String> lastCorrelationId;

    @Override
    public void open(Configuration parameters) {
        turbinePowerKw = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("turbine-power-kw", String.class, Double.class));
        gridDemandMw = getRuntimeContext().getState(new ValueStateDescriptor<>("grid-demand-mw", Double.class));
        lastCorrelationId = getRuntimeContext().getState(
                new ValueStateDescriptor<>("last-correlation-id", String.class));
    }

    @Override
    public void processElement(StreamEvent event, Context ctx, Collector<String> out) throws Exception {
        lastCorrelationId.update(event.correlationId());

        switch (event.kind()) {
            case TURBINE -> updateTurbine(event.turbine());
            case GRID -> gridDemandMw.update(event.grid().demandMW());
            case WEATHER -> {
                // Weather is not required for the energy-state snapshot in v1.
            }
            default -> throw new IllegalStateException("Unexpected event kind: " + event.kind());
        }

        emitIfReady(ctx.getCurrentKey(), out, event.eventTime());
    }

    private void updateTurbine(Turbine turbine) throws Exception {
        turbinePowerKw.put(turbine.turbineId(), turbine.powerOutput());
    }

    private void emitIfReady(String region, Collector<String> out, Instant eventTime) throws Exception {
        Double demand = gridDemandMw.value();
        if (demand == null) {
            return;
        }

        double totalWindPower = sumTurbinePower();
        if (totalWindPower <= 0) {
            return;
        }

        double efficiency = computeEfficiency(totalWindPower, demand);
        var state = new EnergyState(region, totalWindPower, demand, efficiency, eventTime);

        String correlationId = lastCorrelationId.value() != null ? lastCorrelationId.value() : "";
        out.collect(EventEnvelopeJson.serializeEnergyState(region, state, correlationId));
    }

    private double sumTurbinePower() throws Exception {
        double total = 0;
        for (Map.Entry<String, Double> entry : turbinePowerKw.entries()) {
            total += entry.getValue();
        }
        return total;
    }

    static double computeEfficiency(double totalWindPowerKw, double gridDemandMw) {
        double demandKw = gridDemandMw * 1000.0;
        if (demandKw <= 0) {
            return 0.0;
        }
        return Math.min(1.0, totalWindPowerKw / demandKw);
    }
}
