package com.aetherstream.infrastructure.web.dto;

import java.util.UUID;

/** Response returned after a successful ingest command. */
public record IngestResponse(UUID eventId, String correlationId, String status) {
}
