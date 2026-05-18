-- ── tenant_id TEXT → UUID 변환 ───────────────────
ALTER TABLE card_decks  ALTER COLUMN tenant_id TYPE UUID USING tenant_id::uuid;
ALTER TABLE cards        ALTER COLUMN tenant_id TYPE UUID USING tenant_id::uuid;
ALTER TABLE card_reviews ALTER COLUMN tenant_id TYPE UUID USING tenant_id::uuid;
