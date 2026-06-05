package com.aetherstream.datasource.grid.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WriteSideIngestClient {

    private final RestClient restClient;

    public WriteSideIngestClient(
            RestClient.Builder restClientBuilder,
            @Value("${aetherstream.write-side.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public void postGrid(GridPayload payload) {
        restClient
                .post()
                .uri("/api/ingest/grid")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public record GridPayload(String region, double demandMW, double supplyMW) {}
}
