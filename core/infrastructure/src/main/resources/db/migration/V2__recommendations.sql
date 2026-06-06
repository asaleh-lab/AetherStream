-- Read model: optimization recommendations from the decision-engine stream.
CREATE TABLE recommendations (
    id         UUID         PRIMARY KEY,
    region     TEXT         NOT NULL,
    suggestion TEXT         NOT NULL,
    timestamp  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_time
    ON recommendations (timestamp DESC);
