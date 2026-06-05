package com.aetherstream.outbox.relay;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayScheduler {

    private final OutboxRelayService relayService;

    public OutboxRelayScheduler(OutboxRelayService relayService) {
        this.relayService = relayService;
    }

    @Scheduled(fixedDelayString = "${aetherstream.outbox.poll-interval-ms}")
    public void pollAndRelay() {
        relayService.relayBatch();
    }
}
