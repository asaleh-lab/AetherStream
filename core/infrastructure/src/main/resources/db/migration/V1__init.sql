-- AetherStream initial schema.
-- Conventions: snake_case columns, UUID primary keys where applicable, timestamptz for time.

-- ---------------------------------------------------------------------------
-- Transactional outbox (reliability core).
-- Written in the same transaction as the domain state change; drained by the relay.
-- ---------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id             UUID         PRIMARY KEY,
    aggregate_type TEXT         NOT NULL,
    aggregate_id   TEXT         NOT NULL,
    event_type     TEXT         NOT NULL,
    payload        JSONB        NOT NULL,
    status         TEXT         NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ
);

-- Efficient oldest-first polling of unpublished rows by the relay.
CREATE INDEX idx_outbox_pending
    ON outbox_events (created_at)
    WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Write model: latest known turbine state.
-- ---------------------------------------------------------------------------
CREATE TABLE turbine_state (
    turbine_id      TEXT         PRIMARY KEY,
    rpm             DOUBLE PRECISION NOT NULL,
    power_output    DOUBLE PRECISION NOT NULL,
    vibration_level DOUBLE PRECISION NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Read model: aggregated energy-state snapshots per region.
-- ---------------------------------------------------------------------------
CREATE TABLE energy_state_snapshot (
    id               UUID         PRIMARY KEY,
    region           TEXT         NOT NULL,
    total_wind_power DOUBLE PRECISION NOT NULL,
    grid_demand      DOUBLE PRECISION NOT NULL,
    efficiency_score DOUBLE PRECISION NOT NULL,
    timestamp        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_energy_state_region_time
    ON energy_state_snapshot (region, timestamp DESC);

-- ---------------------------------------------------------------------------
-- Read model: alerts raised by the anomaly-detection stream.
-- ---------------------------------------------------------------------------
CREATE TABLE alerts (
    id        UUID         PRIMARY KEY,
    type      TEXT         NOT NULL,
    severity  TEXT         NOT NULL,
    source    TEXT         NOT NULL,
    message   TEXT         NOT NULL,
    timestamp TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_time
    ON alerts (timestamp DESC);
