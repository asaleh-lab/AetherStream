package com.aetherstream.ingestion.weather.polling;

import com.aetherstream.application.command.RecordWeatherReadingCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.model.WeatherReading;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls a weather API on a fixed interval. The skeleton logs the configured URL and falls
 * back to simulated readings until a live HTTP client is wired in a later phase.
 */
@Component
@ConditionalOnProperty(name = "aetherstream.weather.polling.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherPollingService {

    private static final Logger log = LoggerFactory.getLogger(WeatherPollingService.class);
    private static final String DEFAULT_REGION = "north-sea";

    private final CommandBus commandBus;
    private final CorrelationIdContext correlationIdContext;
    private final String apiUrl;

    public WeatherPollingService(
            CommandBus commandBus,
            CorrelationIdContext correlationIdContext,
            @Value("${aetherstream.weather.api-url:https://api.open-meteo.com/v1/forecast}") String apiUrl) {
        this.commandBus = commandBus;
        this.correlationIdContext = correlationIdContext;
        this.apiUrl = apiUrl;
    }

    @Scheduled(fixedDelayString = "${aetherstream.weather.poll-interval-ms:60000}")
    public void poll() {
        try {
            WeatherReading reading = fetchReading();
            String correlationId = correlationIdContext.getOrCreate();
            commandBus.dispatch(new RecordWeatherReadingCommand(reading, correlationId));
            log.info(
                    "Recorded weather reading for {} (correlationId={}, apiUrl={})",
                    reading.region(),
                    correlationId,
                    apiUrl);
        } catch (Exception e) {
            log.warn("Weather poll failed, will retry on next interval (apiUrl={})", apiUrl, e);
        }
    }

    private WeatherReading fetchReading() {
        // Skeleton: simulated reading; replace with RestClient call to apiUrl in a later iteration.
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new WeatherReading(
                DEFAULT_REGION,
                4 + random.nextDouble(0, 12),
                -2 + random.nextDouble(0, 18),
                Instant.now());
    }
}
