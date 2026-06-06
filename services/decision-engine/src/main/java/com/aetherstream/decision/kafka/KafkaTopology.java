package com.aetherstream.decision.kafka;

import com.aetherstream.decision.config.DecisionEngineConfig;
import com.aetherstream.decision.process.RecommendationFunction;
import com.aetherstream.domain.event.Topics;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/** Builds Kafka source and sink for the decision-engine job. */
public final class KafkaTopology {

    private KafkaTopology() {}

    public static void wire(StreamExecutionEnvironment env, DecisionEngineConfig config) {
        DataStream<String> energyStates = source(env, config, Topics.ENERGY_STATE_EVENTS);

        DataStream<String> recommendations =
                energyStates.flatMap(new RecommendationFunction(config.efficiencyTarget()));

        recommendations.sinkTo(sink(config, Topics.RECOMMENDATIONS));
    }

    private static DataStream<String> source(
            StreamExecutionEnvironment env, DecisionEngineConfig config, String topic) {
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(config.kafkaBootstrapServers())
                .setTopics(topic)
                .setGroupId(DecisionEngineConfig.CONSUMER_GROUP)
                .setStartingOffsets(config.offsetReset() == DecisionEngineConfig.OffsetReset.EARLIEST
                        ? OffsetsInitializer.earliest()
                        : OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        return env.fromSource(kafkaSource, org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), topic + "-source");
    }

    private static KafkaSink<String> sink(DecisionEngineConfig config, String topic) {
        return KafkaSink.<String>builder()
                .setBootstrapServers(config.kafkaBootstrapServers())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(topic)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();
    }
}
