package com.aetherstream.datasource.client;

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

    public void postTurbine(TurbinePayload payload) {
        post("/api/ingest/turbine", payload);
    }

    public void postGrid(GridPayload payload) {
        post("/api/ingest/grid", payload);
    }

    private void post(String path, Object body) {
        restClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public record TurbinePayload(String turbineId, double rpm, double powerOutput, double vibrationLevel) {}

    public record GridPayload(String region, double demandMW, double supplyMW) {}
}
