# AetherStream Constitution

AetherStream is a real-time wind-energy monitoring platform. It is a portfolio-grade
system whose primary purpose is to demonstrate correct, production-shaped design of an
event-driven streaming architecture on the JVM, with a separate .NET real-time UI.
These principles are binding on all specs, plans, and implementation work.

## Core Principles

### I. Event-Driven Backbone
Kafka is the system's source of truth for inter-service communication. Services
communicate through topics, not synchronous calls, except for query-side reads.
Every cross-service state change is represented as an immutable, versioned event.
Topic names, keys, and payload schemas are defined in the spec before code is written.

### II. Reliable Publishing via the Outbox Pattern (NON-NEGOTIABLE)
No service performs a "dual write" (DB commit plus direct Kafka publish) inside a
business transaction. State changes and their resulting events are written to the
`outbox_events` table in the SAME database transaction. A separate relay publishes
outbox rows to Kafka with at-least-once delivery, retries, and a dead-letter path.
Downstream consumers MUST be idempotent. This guarantee is not optional and may not
be bypassed for convenience.

### III. CQRS Separation
The write model (commands, domain state, outbox) and the read model (query-optimized
projections) are kept strictly separate. Commands never serve reads; queries never
mutate state. Command and query handlers are dispatched through explicit bus
abstractions (the MediatR-equivalent) rather than direct service coupling.

### IV. Clean, DDD-Inspired Layering
Code is organized as `domain` (pure model, no framework deps), `application` (use
cases, command/query handlers, ports), and `infrastructure` (JPA, Kafka, adapters).
Dependencies point inward: domain depends on nothing; infrastructure depends on
application and domain, never the reverse.

### V. Observability by Default
Structured JSON logging is mandatory. A correlation ID is generated at ingestion and
propagated across API -> DB -> outbox -> Kafka -> stream processing via MDC and Kafka
headers. Every deployable exposes health and metrics endpoints. A change is not "done"
until it is observable.

### VI. Spec-Driven Development
Work follows the spec-kit flow: constitution -> specify -> plan -> tasks -> implement.
Specs and architecture decisions are committed artifacts, authored before the code that
realizes them. Cross-chat continuity is maintained in `HANDOFF.md`.

### VII. Test the Risk, Not the Framework
Reliability-critical paths (outbox relay correctness, CQRS handlers, Kafka
producers/consumers, stream processors) are covered by integration tests using
Testcontainers for Kafka and PostgreSQL. Trivial glue code is not over-tested.

## Technology Constraints

- Backend: Java 21, Spring Boot 3.3.x, Apache Flink 1.19 (stream processing).
- Messaging: Apache Kafka in KRaft mode. Persistence: PostgreSQL 16 with Flyway migrations.
- Build: Maven multi-module reactor; a committed Maven Wrapper makes the repo self-contained.
- UI: .NET 10 Blazor Server with Radzen components, consuming REST + WebSocket from the gateway.
- Infrastructure is reproducible via a single `docker-compose` for Kafka and PostgreSQL.
- The Java mapping table in the project brief (ASP.NET -> JVM equivalents) is documentation
  intent; the implementation is JVM-native, not a literal .NET port.

## Development Workflow

- Branching: phase-based feature branches merged into `main` via pull request.
- Commits: Conventional Commits (`feat`, `fix`, `build`, `chore`, `docs`, `test`, `ci`),
  small and single-concern, each in a compiling/validating state.
- Each phase ends by updating `HANDOFF.md`, running the phase's verification, and opening a PR.
- The system is built in phases; no phase claims completion with a broken build.

## Governance

This constitution supersedes ad-hoc preferences. Any deviation (for example, a justified
synchronous call or a skipped test) must be documented in the relevant spec or PR with its
rationale. Amendments are made by editing this file with a version bump and a dated note.

**Version**: 1.0.1 | **Ratified**: 2026-06-05 | **Last Amended**: 2026-06-05
