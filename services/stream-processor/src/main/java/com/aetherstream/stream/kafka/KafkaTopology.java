package com.aetherstream.stream.kafka;

import com.aetherstream.domain.event.Topics;
import com.aetherstream.stream.aggregation.EnergyAggregationFunction;
import com.aetherstream.stream.anomaly.AnomalyDetectionFunction;
import com.aetherstream.stream.config.StreamProcessorConfig;
import com.aetherstream.stream.ingest.IngestEventMapper;
import com.aetherstream.stream.model.StreamEvent;
import java.time.Duration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/** Builds Kafka sources, processing branches, and sinks for the stream-processor job. */
public final class KafkaTopology {

    private static final Duration ALLOWED_LATENESS = Duration.ofSeconds(5);

    private KafkaTopology() {}

    public static void wire(StreamExecutionEnvironment env, StreamProcessorConfig config) {
        DataStream<String> turbineRaw = source(env, config, Topics.TURBINE_EVENTS);
        DataStream<String> weatherRaw = source(env, config, Topics.WEATHER_EVENTS);
        DataStream<String> gridRaw = source(env, config, Topics.GRID_EVENTS);

        DataStream<StreamEvent> ingest = turbineRaw
                .union(weatherRaw, gridRaw)
                .map(new IngestEventMapper())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<StreamEvent>forBoundedOutOfOrderness(ALLOWED_LATENESS)
                                .withTimestampAssigner((event, ts) -> event.eventTime().toEpochMilli()));

        DataStream<String> energyStates = ingest
                .keyBy(StreamEvent::region)
                .process(new EnergyAggregationFunction());

        DataStream<String> alerts = ingest
                .filter(event -> event.kind() != com.aetherstream.stream.model.StreamEventKind.WEATHER)
                .flatMap(new AnomalyDetectionFunction(config.vibrationThreshold()));

        energyStates.sinkTo(sink(config, Topics.ENERGY_STATE_EVENTS));
        alerts.sinkTo(sink(config, Topics.ALERTS));
    }

    private static DataStream<String> source(
            StreamExecutionEnvironment env, StreamProcessorConfig config, String topic) {
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(config.kafkaBootstrapServers())
                .setTopics(topic)
                .setGroupId(StreamProcessorConfig.CONSUMER_GROUP + "-" + topic)
                .setStartingOffsets(config.offsetReset() == StreamProcessorConfig.OffsetReset.EARLIEST
                        ? OffsetsInitializer.earliest()
                        : OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        return env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), topic + "-source");
    }

    private static KafkaSink<String> sink(StreamProcessorConfig config, String topic) {
        return KafkaSink.<String>builder()
                .setBootstrapServers(config.kafkaBootstrapServers())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(topic)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();
    }
}
