package com.aetherstream.decision;

import com.aetherstream.decision.config.DecisionEngineConfig;
import com.aetherstream.decision.kafka.KafkaTopology;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flink job entry point for the decision engine.
 *
 * <p>Topology:
 * <ul>
 *   <li>Consume {@code energy-state-events}.</li>
 *   <li>Apply optimization rules to produce turbine-adjustment and grid-balancing
 *       recommendations.</li>
 *   <li>Emit recommendations to {@code recommendations} for the gateway/UI.</li>
 * </ul>
 */
public class DecisionEngineJob {

    private static final Logger log = LoggerFactory.getLogger(DecisionEngineJob.class);

    public static void main(String[] args) throws Exception {
        run(DecisionEngineConfig.fromEnvironment());
    }

    public static void run(DecisionEngineConfig config) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        log.info(
                "Starting decision-engine (kafka={}, efficiencyTarget={}, offsetReset={})",
                config.kafkaBootstrapServers(),
                config.efficiencyTarget(),
                config.offsetReset());

        KafkaTopology.wire(env, config);
        env.execute("aetherstream-decision-engine");
    }

    private DecisionEngineJob() {}
}
