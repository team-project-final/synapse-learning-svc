-- ── 인덱스 먼저 삭제 (status 컬럼 참조) ─────────────
DROP INDEX IF EXISTS idx_cards_due;

-- ── 컬럼 이름 변경 ─────────────────────────────────
ALTER TABLE cards RENAME COLUMN front TO front_content;
ALTER TABLE cards RENAME COLUMN back  TO back_content;
ALTER TABLE cards RENAME COLUMN state TO status;
ALTER TABLE cards RENAME COLUMN next_review_at TO due_date;

-- ── SRS 개별 컬럼 추가 ────────────────────────────
ALTER TABLE cards ADD COLUMN easiness_factor NUMERIC(4,2) NOT NULL DEFAULT 2.5;
ALTER TABLE cards ADD COLUMN interval_days   INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE cards ADD COLUMN repetitions     INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE cards ADD COLUMN lapses          INTEGER      NOT NULL DEFAULT 0;

-- ── 불필요 컬럼 제거 ──────────────────────────────
ALTER TABLE cards DROP COLUMN srs_state;
ALTER TABLE cards DROP COLUMN srs_algorithm;
ALTER TABLE cards DROP COLUMN extra;

-- ── 제약조건 재생성 ───────────────────────────────
ALTER TABLE cards DROP CONSTRAINT IF EXISTS chk_cards_state;
ALTER TABLE cards DROP CONSTRAINT IF EXISTS chk_cards_srs_algorithm;
ALTER TABLE cards ADD CONSTRAINT chk_cards_status
    CHECK (status IN ('new', 'learning', 'review', 'relearning', 'suspended'));

-- ── 인덱스 재생성 (status 컬럼 기준) ─────────────
CREATE INDEX idx_cards_due ON cards (tenant_id, deck_id, due_date)
    WHERE status IN ('learning', 'review', 'relearning')
      AND deleted_at IS NULL;
