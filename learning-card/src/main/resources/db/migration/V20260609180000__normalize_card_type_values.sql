-- card_type 값을 소문자 canonical 값으로 정규화
-- 순서 중요: 기존 제약 DROP → 데이터 UPDATE → 새 제약 ADD

ALTER TABLE cards DROP CONSTRAINT IF EXISTS chk_cards_card_type;

UPDATE cards SET card_type = 'basic'
WHERE UPPER(card_type) IN ('QA', 'AI_GENERATED', 'BASIC');

UPDATE cards SET card_type = 'cloze'
WHERE UPPER(card_type) = 'CLOZE' AND card_type <> 'cloze';

UPDATE cards SET card_type = 'definition'
WHERE UPPER(card_type) = 'DEFINITION' AND card_type <> 'definition';

ALTER TABLE cards ADD CONSTRAINT chk_cards_card_type
    CHECK (card_type IN ('basic', 'cloze', 'definition'));
