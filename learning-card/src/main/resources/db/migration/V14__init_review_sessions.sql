CREATE TABLE review_sessions
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    deck_id      UUID        NOT NULL REFERENCES card_decks (id),
    status       VARCHAR(20) NOT NULL DEFAULT 'in_progress',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    total_cards  INTEGER     NOT NULL DEFAULT 0,
    reviewed_cards INTEGER   NOT NULL DEFAULT 0,

    CONSTRAINT chk_session_status CHECK (status IN ('in_progress', 'completed', 'abandoned'))
);

CREATE INDEX idx_sessions_tenant_user ON review_sessions (tenant_id, user_id, started_at DESC);
CREATE INDEX idx_sessions_deck ON review_sessions (deck_id, status);