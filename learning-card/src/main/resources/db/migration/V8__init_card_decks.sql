-- card_decks 테이블 생성
CREATE TABLE card_decks (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    color       CHAR(7)     NOT NULL DEFAULT '#4A90E2',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- 인덱스: 같은 사용자가 같은 이름의 덱 중복 생성 방지
CREATE UNIQUE INDEX uq_decks_tenant_user_name
    ON card_decks (tenant_id, user_id, name)
    WHERE deleted_at IS NULL;