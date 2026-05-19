ALTER TABLE card_reviews
    ADD COLUMN session_id UUID REFERENCES review_sessions (id);

CREATE INDEX idx_reviews_session ON card_reviews (session_id);