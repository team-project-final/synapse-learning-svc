package com.synapse.learning.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증 실패"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류"),

    // ── 덱(Deck) ──────────────────────────────────
    DECK_NOT_FOUND(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다"),
    DECK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "덱에 접근 권한이 없습니다"),

    // ── 카드(Card) ────────────────────────────────
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다"),
    CARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "카드에 접근 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}