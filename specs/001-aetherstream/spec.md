# Feature Specification: AetherStream Wind-Energy Real-Time Monitoring Platform

**Feature Branch**: `phase-1/infra-skeleton`

**Created**: 2026-06-05

**Status**: Complete

**Input**: Portfolio brief: a production-shaped, event-driven streaming platform for wind
energy monitoring built on Kafka + CQRS + Outbox + Flink-style stream processing (JVM),
with a .NET Blazor + Radzen real-time UI.

## Overview

AetherStream ingests two real-time data streams (wind turbine telemetry and grid load),
processes them through a streaming pipeline, and produces aggregated energy state, anomaly
alerts, and optimization recommendations. An operator watches a live dashboard that updates
without manual refresh.

The system exists to demonstrate correct design of: an event-driven Kafka backbone,
reliable publishing via the Outbox pattern, CQRS separation, stream processing
(Flink-style joins/windows), and an integrated real-time UI.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reliable ingestion without event loss (Priority: P1)

As the platform, when a telemetry or grid reading is ingested, the reading is
persisted and a corresponding event is reliably published to Kafka exactly once from the
producer's perspective, even if the process crashes immediately after the database commit.

**Why this priority**: This is the reliability core of the system. Without guaranteed
publishing, every downstream computation is suspect. It is the headline engineering claim
of the project (Outbox pattern, no dual-write).

**Independent Test**: Ingest a reading, kill the service after the DB commit but before
any relay run, restart, and confirm the event still reaches Kafka exactly once (idempotent
consumer dedupes any at-least-once retry). Verifiable with Testcontainers (Postgres + Kafka).

**Acceptance Scenarios**:

1. **Given** a turbine reading is POSTed, **When** the command transaction commits, **Then** a row exists in `outbox_events` with status `PENDING` in the same transaction as the domain state change.
2. **Given** a `PENDING` outbox row, **When** the relay runs, **Then** the event is published to the correct Kafka topic and the row is marked `SENT` with a `processed_at` timestamp.
3. **Given** the relay fails to publish, **When** retries are exhausted, **Then** the event is routed to `dead-letter-events` and the row is marked `FAILED`.
4. **Given** the same event is delivered twice, **When** a downstream consumer processes it, **Then** the effect is applied at most once (idempotent by event id).

### User Story 2 - Live energy state on the dashboard (Priority: P1)

As an operator, I open the dashboard and see the current aggregated energy state per
region (total wind power, grid demand, efficiency score), updating in real time as new
events flow, without refreshing the page.

**Why this priority**: This is the primary user-facing value and exercises the full
pipeline end to end (ingest -> outbox -> Kafka -> stream join -> read model -> WebSocket -> UI).

**Independent Test**: With the pipeline running, push synthetic turbine/grid
events and observe the dashboard energy cards and efficiency gauge update within the
target latency, with no manual refresh.

**Acceptance Scenarios**:

1. **Given** turbine and grid events for a region, **When** the aggregation stream processes a window, **Then** an `energy-state-events` record is produced with `region`, `totalWindPower`, `gridDemand`, and `efficiencyScore`.
2. **Given** a new energy-state event, **When** it reaches the gateway, **Then** the dashboard updates the affected region's cards via WebSocket push.

### User Story 3 - Real-time anomaly alerts (Priority: P2)

As an operator, I am alerted in real time when the system detects a vibration spike, a
turbine failure pattern, or grid overload risk, with severity-based visual styling.

**Why this priority**: Demonstrates the anomaly-detection stream and the alerting path;
high value but depends on the aggregation pipeline (P1) being in place.

**Independent Test**: Feed a turbine event with `vibrationLevel` above threshold and
confirm an alert appears in the alerts panel with the correct severity, sourced from the
`alerts` topic.

**Acceptance Scenarios**:

1. **Given** a turbine event whose vibration exceeds the configured threshold, **When** the anomaly stream evaluates it, **Then** an alert event is emitted to `alerts`.
2. **Given** sustained grid demand exceeding supply, **When** the overload rule triggers, **Then** a `GRID_OVERLOAD_RISK` alert is emitted.
3. **Given** a new alert, **When** it reaches the gateway, **Then** the alerts panel renders it with severity-based styling in real time.

### User Story 4 - Optimization recommendations (Priority: P3)

As an operator, I receive optimization recommendations (turbine adjustment suggestions,
grid balancing signals) derived from the current energy state.

**Why this priority**: The decision-engine layer; valuable but built on top of aggregation
and anomaly detection.

**Independent Test**: Given a known low-efficiency energy state, confirm the decision
engine emits a recommendation with an actionable suggestion.

**Acceptance Scenarios**:

1. **Given** an energy state with efficiency below target, **When** the decision engine evaluates it, **Then** a recommendation is produced describing the suggested adjustment.

### User Story 5 - Turbine monitoring view (Priority: P3)

As an operator, I view a turbine health grid (health status, vibration indicators).

**Independent Test**: Query the turbine read model and confirm the Radzen DataGrid shows
per-turbine state.

**Acceptance Scenarios**:

1. **Given** persisted turbine state, **When** the operator opens turbine monitoring, **Then** a grid lists each turbine with current health and vibration indicators.

### Edge Cases

- A reading arrives for an unknown region or turbine id -> ingested and tagged; aggregation tolerates sparse partners.
- Kafka is temporarily unavailable -> outbox rows accumulate as `PENDING` and drain when Kafka returns; no data loss.
- Duplicate delivery from at-least-once semantics -> consumers dedupe by event id.
- Late or out-of-order events within a window -> handled by the stream's windowing/watermark policy.
- Relay processes a batch and crashes mid-batch -> rows not marked `SENT` are reprocessed; consumers stay idempotent.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST ingest wind turbine telemetry (`turbineId`, `rpm`, `powerOutput`, `vibrationLevel`) via a command API and a simulated producer.
- **FR-002**: System MUST ingest grid load (`region`, `demandMW`, `supplyMW`) from a simulated real-time feed.
- **FR-003**: On every state-changing command, System MUST write the domain state change and an `outbox_events` row within a single database transaction, and MUST NOT publish to Kafka inside that transaction.
- **FR-004**: A relay service MUST poll `outbox_events`, publish `PENDING` rows to Kafka with batching and retries, mark them `SENT`, and route exhausted failures to `dead-letter-events` (`FAILED`).
- **FR-005**: Delivery to Kafka MUST be at-least-once; downstream consumers MUST be idempotent (dedupe by event id).
- **FR-006**: System MUST join turbine and grid streams to produce `energy-state-events` containing `timestamp`, `region`, `totalWindPower`, `gridDemand`, `efficiencyScore`.
- **FR-007**: System MUST detect anomalies (vibration spikes, turbine failure patterns, grid overload risk) and emit alerts to the `alerts` topic.
- **FR-008**: System MUST produce optimization recommendations from the current energy state.
- **FR-009**: System MUST expose command APIs (`POST /api/ingest/turbine`, `/api/ingest/grid`) and query APIs (`GET /api/energy/latest`, `/api/alerts`, `/api/turbines/{id}`).
- **FR-010**: System MUST push energy-state updates and alerts to clients over WebSocket.
- **FR-011**: Read and write models MUST be separated (CQRS); commands MUST NOT serve reads and queries MUST NOT mutate state.
- **FR-012**: System MUST persist turbine state, energy-state snapshots, alerts, and `outbox_events` in PostgreSQL, with schema managed by Flyway.
- **FR-013**: System MUST emit structured JSON logs and propagate a correlation id across API -> DB -> outbox -> Kafka -> stream processing.
- **FR-014**: Each deployable MUST expose health and metrics endpoints.
- **FR-015**: The Blazor + Radzen UI MUST update reactively via WebSocket with no manual refresh.

### Key Entities *(include if feature involves data)*

- **Turbine**: a wind turbine and its latest telemetry (`turbineId`, `rpm`, `powerOutput`, `vibrationLevel`, health status).
- **GridLoad**: grid demand/supply for a region (`region`, `demandMW`, `supplyMW`, timestamp).
- **EnergyState**: aggregated per-region state (`region`, `totalWindPower`, `gridDemand`, `efficiencyScore`, `timestamp`).
- **Alert**: a detected condition (type, severity, source, timestamp, message).
- **Recommendation**: an optimization suggestion derived from energy state.
- **OutboxEvent**: a reliably-published event row (`id`, `aggregateType`, `aggregateId`, `eventType`, `payload`, `status`, `createdAt`, `processedAt`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After ingestion, 100% of committed readings result in exactly one logical event reaching Kafka (no loss; duplicates deduped downstream), verified under a crash-after-commit test.
- **SC-002**: An ingested reading is reflected in the dashboard's live energy state within 5 seconds end to end under nominal local load.
- **SC-003**: A vibration value above threshold produces a visible severity-styled alert within 5 seconds.
- **SC-004**: The full local environment (Kafka + Postgres + all services + UI) starts from `docker-compose` plus documented run steps with no manual schema setup (Flyway applies migrations automatically).
- **SC-005**: Reliability-critical paths (outbox relay, CQRS handlers, producers/consumers, stream processors) have passing Testcontainers-backed integration tests.

## Assumptions

- This is a portfolio/demonstration system; scale targets are local/single-node, not multi-region production load.
- Data sources are simulated (turbine/grid producers); no real SCADA integration.
- Kafka runs in KRaft mode (no ZooKeeper); a single broker is sufficient for the demo.
- Stream processing is implemented with Apache Flink (or Flink-style semantics) on the JVM; "Flink-style" means real windowing/join/keyed-state semantics, not a literal port.
- The UI is .NET 10 Blazor Server with Radzen, a separate process from the JVM backend, communicating over REST + WebSocket.
- Exactly-once is achieved at the system level via at-least-once delivery plus idempotent consumers, not via Kafka transactional exactly-once across all hops.
