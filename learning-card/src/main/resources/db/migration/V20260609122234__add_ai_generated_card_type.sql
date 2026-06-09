-- card_type CHECK 제약에 AI_GENERATED 허용 추가
-- learning-ai 서비스가 생성한 카드를 learning-card에 저장할 때 사용
ALTER TABLE cards DROP CONSTRAINT chk_cards_card_type;
ALTER TABLE cards ADD CONSTRAINT chk_cards_card_type
    CHECK (card_type IN ('qa', 'cloze', 'definition', 'AI_GENERATED'));
