package com.aetherstream.infrastructure.cqrs;

import com.aetherstream.application.cqrs.Query;
import com.aetherstream.application.cqrs.QueryBus;
import com.aetherstream.application.cqrs.QueryHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SpringQueryBus implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers;

    public SpringQueryBus(List<QueryHandler<?, ?>> handlers) {
        this.handlers =
                handlers.stream().collect(Collectors.toMap(QueryHandler::queryType, Function.identity()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, Q extends Query<R>> R dispatch(Q query) {
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for query: " + query.getClass().getName());
        }
        return handler.handle(query);
    }
}
