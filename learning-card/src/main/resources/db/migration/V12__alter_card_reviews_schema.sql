-- ── card_reviews 테이블 재생성 (id UUID, repetitions 제거, time_spent_ms 추가) ──
DROP TABLE IF EXISTS card_reviews;

CREATE TABLE card_reviews
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        TEXT             NOT NULL,
    card_id          UUID             NOT NULL REFERENCES cards (id),
    rating           SMALLINT         NOT NULL,
    prev_ease_factor DOUBLE PRECISION NOT NULL,
    new_ease_factor  DOUBLE PRECISION NOT NULL,
    prev_interval    INTEGER          NOT NULL,
    new_interval     INTEGER          NOT NULL,
    time_spent_ms    INTEGER,
    reviewed_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_card_reviews_rating CHECK (rating BETWEEN 1 AND 4)
);

CREATE INDEX idx_reviews_tenant_card_time ON card_reviews (tenant_id, card_id, reviewed_at DESC);
CREATE INDEX idx_reviews_tenant_time ON card_reviews (tenant_id, reviewed_at DESC);
