package com.aetherstream.decision.config;

/** Runtime configuration for the Flink decision-engine job (env vars with local defaults). */
public final class DecisionEngineConfig {

    public static final String KAFKA_BOOTSTRAP_ENV = "AETHER_KAFKA_BOOTSTRAP";
    public static final String EFFICIENCY_TARGET_ENV = "AETHER_EFFICIENCY_TARGET";
    public static final String KAFKA_OFFSET_RESET_ENV = "AETHER_KAFKA_OFFSET_RESET";
    public static final String CONSUMER_GROUP = "aetherstream-decision-engine";

    public enum OffsetReset {
        EARLIEST,
        LATEST
    }

    private final String kafkaBootstrapServers;
    private final double efficiencyTarget;
    private final OffsetReset offsetReset;

    public DecisionEngineConfig(
            String kafkaBootstrapServers, double efficiencyTarget, OffsetReset offsetReset) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.efficiencyTarget = efficiencyTarget;
        this.offsetReset = offsetReset;
    }

    public static DecisionEngineConfig fromEnvironment() {
        String bootstrap = System.getenv().getOrDefault(KAFKA_BOOTSTRAP_ENV, "localhost:9094");
        double target = Double.parseDouble(System.getenv().getOrDefault(EFFICIENCY_TARGET_ENV, "0.85"));
        OffsetReset offsetReset =
                "earliest".equalsIgnoreCase(System.getenv().get(KAFKA_OFFSET_RESET_ENV))
                        ? OffsetReset.EARLIEST
                        : OffsetReset.LATEST;
        return new DecisionEngineConfig(bootstrap, target, offsetReset);
    }

    public String kafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public double efficiencyTarget() {
        return efficiencyTarget;
    }

    public OffsetReset offsetReset() {
        return offsetReset;
    }
}
