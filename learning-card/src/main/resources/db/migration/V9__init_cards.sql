CREATE TABLE cards
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    tenant_id        TEXT        NOT NULL,
    deck_id          UUID        NOT NULL REFERENCES card_decks (id),
    source_type      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    source_id        UUID,
    card_type        VARCHAR(20) NOT NULL,
    front            TEXT        NOT NULL,
    back             TEXT        NOT NULL,
    bloom_level      VARCHAR(20),
    srs_algorithm    VARCHAR(20) NOT NULL DEFAULT 'SM2',
    srs_state        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    next_review_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_reviewed_at TIMESTAMPTZ,
    state            VARCHAR(20) NOT NULL DEFAULT 'new',
    extra            JSONB       NOT NULL DEFAULT '{}'::jsonb,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_cards_tenant_deck ON cards (tenant_id, deck_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cards_source ON cards (tenant_id, source_type, source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_cards_due ON cards (tenant_id, deck_id, next_review_at)
    WHERE state IN ('learning', 'review', 'relearning')
      AND deleted_at IS NULL;

ALTER TABLE cards
    ADD CONSTRAINT chk_cards_card_type CHECK (card_type IN ('qa', 'cloze', 'definition'));
ALTER TABLE cards
    ADD CONSTRAINT chk_cards_state CHECK (state IN ('new', 'learning', 'review', 'relearning', 'suspended'));
ALTER TABLE cards
    ADD CONSTRAINT chk_cards_source_type CHECK (source_type IN ('NOTE', 'MANUAL', 'IMPORT', 'MULTI', 'NOTE_DELETED'));
ALTER TABLE cards
    ADD CONSTRAINT chk_cards_srs_algorithm CHECK (srs_algorithm IN ('SM2', 'FSRS', 'LEITNER'));