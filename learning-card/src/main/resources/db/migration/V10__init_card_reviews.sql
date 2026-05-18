CREATE TABLE card_reviews
(
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       TEXT        NOT NULL,
    card_id         UUID        NOT NULL REFERENCES cards (id),
    rating          SMALLINT    NOT NULL,
    prev_ease_factor DOUBLE PRECISION NOT NULL,
    new_ease_factor  DOUBLE PRECISION NOT NULL,
    prev_interval   INTEGER     NOT NULL,
    new_interval    INTEGER     NOT NULL,
    repetitions     INTEGER     NOT NULL,
    reviewed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_card_reviews_rating CHECK (rating BETWEEN 1 AND 4)
);

CREATE INDEX idx_reviews_tenant_card_time ON card_reviews(tenant_id, card_id, reviewed_at DESC);
CREATE INDEX idx_reviews_tenant_time ON card_reviews(tenant_id, reviewed_at DESC);