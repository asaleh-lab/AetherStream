package com.aetherstream.stream;

import com.aetherstream.stream.config.StreamProcessorConfig;
import com.aetherstream.stream.kafka.KafkaTopology;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flink job entry point for the stream-processing layer.
 *
 * <p>Topology:
 * <ul>
 *   <li>Consume {@code turbine-events}, {@code weather-events}, {@code grid-events}.</li>
 *   <li>Key by region; maintain keyed state to compute {@code totalWindPower},
 *       {@code gridDemand}, {@code efficiencyScore}; emit {@code energy-state-events}.</li>
 *   <li>Apply keyed anomaly rules (vibration spike, failure patterns, grid overload) and
 *       emit to {@code alerts}.</li>
 *   <li>Event-time watermarks with bounded out-of-order lateness.</li>
 * </ul>
 */
public class StreamProcessorJob {

    private static final Logger log = LoggerFactory.getLogger(StreamProcessorJob.class);

    public static void main(String[] args) throws Exception {
        run(StreamProcessorConfig.fromEnvironment());
    }

    public static void run(StreamProcessorConfig config) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        log.info(
                "Starting stream-processor (kafka={}, vibrationThreshold={}, offsetReset={})",
                config.kafkaBootstrapServers(),
                config.vibrationThreshold(),
                config.offsetReset());

        KafkaTopology.wire(env, config);
        env.execute("aetherstream-stream-processor");
    }

    private StreamProcessorJob() {}
}
