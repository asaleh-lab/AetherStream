package com.aetherstream.application.cqrs;

/**
 * Marker interface for read-side queries. The type parameter binds a query to the type of
 * result it produces, enabling type-safe dispatch.
 *
 * @param <R> the result type produced by handling this query
 */
public interface Query<R> {
}
