package com.synapse.learning.shared;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        ApiMeta meta) {
    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ApiMeta.now());
    }

    // 실패 응답
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null,
                new ApiError(code, message, List.of()), ApiMeta.now());
    }

    // 실패 응답 (검증 오류 상세 포함)
    public static ApiResponse<Void> error(String code, String message, List<String> details) {
        return new ApiResponse<>(false, null,
                new ApiError(code, message, details), ApiMeta.now());
    }

    // 응답 메타 정보
    public record ApiMeta(Instant timestamp) {
        public static ApiMeta now() {
            return new ApiMeta(Instant.now());
        }
    }

    // 에러 상세 정보
    public record ApiError(String code, String message, List<String> details) {
    }
}