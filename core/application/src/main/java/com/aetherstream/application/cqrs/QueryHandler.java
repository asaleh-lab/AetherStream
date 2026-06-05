package com.aetherstream.application.cqrs;

/**
 * Handles a single type of {@link Query}, returning its result. Implementations live in the
 * read-side (api-gateway) and read only from query-optimized projections.
 *
 * @param <Q> the query type handled
 * @param <R> the result type produced
 */
public interface QueryHandler<Q extends Query<R>, R> {

    R handle(Q query);

    Class<Q> queryType();
}
