package com.aetherstream.infrastructure.correlation;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdContext {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";

    public String getOrCreate() {
        String id = MDC.get(MDC_KEY);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            MDC.put(MDC_KEY, id);
        }
        return id;
    }

    public void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }
}
