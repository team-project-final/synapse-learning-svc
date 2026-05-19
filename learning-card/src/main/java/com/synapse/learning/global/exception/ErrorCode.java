package com.synapse.learning.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다"),
    DECK_NOT_FOUND(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다"),
    DECK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "덱에 대한 접근 권한이 없습니다"),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다"),
    CARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "카드에 대한 접근 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getMessage() { return message; }
}
