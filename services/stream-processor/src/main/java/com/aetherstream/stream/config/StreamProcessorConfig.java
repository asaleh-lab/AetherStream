package com.aetherstream.stream.config;

/**
 * Runtime configuration for the Flink stream-processor job (env vars with local defaults).
 */
public final class StreamProcessorConfig {

    public static final String KAFKA_BOOTSTRAP_ENV = "AETHER_KAFKA_BOOTSTRAP";
    public static final String VIBRATION_THRESHOLD_ENV = "AETHER_VIBRATION_THRESHOLD";
    public static final String KAFKA_OFFSET_RESET_ENV = "AETHER_KAFKA_OFFSET_RESET";
    public static final String CONSUMER_GROUP = "aetherstream-stream-processor";

    public enum OffsetReset {
        EARLIEST,
        LATEST
    }

    private final String kafkaBootstrapServers;
    private final double vibrationThreshold;
    private final OffsetReset offsetReset;

    public StreamProcessorConfig(
            String kafkaBootstrapServers, double vibrationThreshold, OffsetReset offsetReset) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.vibrationThreshold = vibrationThreshold;
        this.offsetReset = offsetReset;
    }

    public static StreamProcessorConfig fromEnvironment() {
        String bootstrap = System.getenv().getOrDefault(KAFKA_BOOTSTRAP_ENV, "localhost:9094");
        double threshold = Double.parseDouble(System.getenv().getOrDefault(VIBRATION_THRESHOLD_ENV, "1.0"));
        OffsetReset offsetReset =
                "earliest".equalsIgnoreCase(System.getenv().get(KAFKA_OFFSET_RESET_ENV))
                        ? OffsetReset.EARLIEST
                        : OffsetReset.LATEST;
        return new StreamProcessorConfig(bootstrap, threshold, offsetReset);
    }

    public String kafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public double vibrationThreshold() {
        return vibrationThreshold;
    }

    public OffsetReset offsetReset() {
        return offsetReset;
    }
}
