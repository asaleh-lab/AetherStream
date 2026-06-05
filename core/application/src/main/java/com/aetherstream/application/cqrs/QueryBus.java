package com.aetherstream.application.cqrs;

/**
 * Dispatches a {@link Query} to its registered {@link QueryHandler} and returns the result.
 */
public interface QueryBus {

    <R, Q extends Query<R>> R dispatch(Q query);
}
