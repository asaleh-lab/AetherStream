package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.OutboxAppender;
import com.aetherstream.domain.outbox.OutboxEvent;
import com.aetherstream.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class JpaOutboxAppender implements OutboxAppender {

    private final OutboxEventRepository repository;

    public JpaOutboxAppender(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(OutboxEvent event) {
        repository.save(OutboxEventMapper.toEntity(event));
    }
}
