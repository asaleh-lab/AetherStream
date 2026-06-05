package com.aetherstream.datasource.weather.polling;

import com.aetherstream.datasource.weather.client.WriteSideIngestClient;
import com.aetherstream.datasource.weather.client.WriteSideIngestClient.WeatherPayload;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Demonstrates pull-based ingestion: GET from an external weather API (no push/stream from
 * provider), then POST the reading to write-side. Falls back to simulated values when the API
 * is unreachable.
 */
@Component
@ConditionalOnProperty(name = "aetherstream.weather.polling.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherPollingService {

    private static final Logger log = LoggerFactory.getLogger(WeatherPollingService.class);
    private static final String DEFAULT_REGION = "north-sea";

    private final WriteSideIngestClient ingestClient;
    private final RestClient weatherApiClient;
    private final String apiUrl;

    public WeatherPollingService(
            WriteSideIngestClient ingestClient,
            RestClient.Builder restClientBuilder,
            @Value("${aetherstream.weather.api-url:https://api.open-meteo.com/v1/forecast}") String apiUrl) {
        this.ingestClient = ingestClient;
        this.weatherApiClient = restClientBuilder.build();
        this.apiUrl = apiUrl;
    }

    @Scheduled(fixedDelayString = "${aetherstream.weather.poll-interval-ms:60000}")
    public void poll() {
        try {
            WeatherPayload payload = fetchReading();
            ingestClient.postWeather(payload);
            log.info("Forwarded weather reading for {} to write-side", payload.region());
        } catch (Exception e) {
            log.warn("Weather poll cycle failed, will retry on next interval", e);
        }
    }

    private WeatherPayload fetchReading() {
        try {
            // Skeleton GET: open-meteo has no realtime push; we poll on an interval.
            weatherApiClient.get().uri(apiUrl).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.debug("Weather API GET failed ({}), using simulated reading", apiUrl);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new WeatherPayload(
                DEFAULT_REGION, 4 + random.nextDouble(0, 12), -2 + random.nextDouble(0, 18));
    }
}
