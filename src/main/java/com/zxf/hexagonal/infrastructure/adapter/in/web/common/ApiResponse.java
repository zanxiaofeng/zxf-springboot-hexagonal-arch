package com.zxf.hexagonal.infrastructure.adapter.in.web.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 统一响应信封。成功 code 固定 "000000"；错误 code 为稳定契约
 * （领域异常 CODE 常量或传输层常量，见 exception-handling.md §6.2）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        T data,
        String message,
        List<ErrorItem> errors,
        OffsetDateTime timestamp,
        String traceId
) {

    public static final String SUCCESS_CODE = "000000";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, null, null, OffsetDateTime.now(), null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, null, message, null, OffsetDateTime.now(), null);
    }

    public static ApiResponse<Void> validationError(List<ErrorItem> errors) {
        return new ApiResponse<>("VALIDATION_ERROR", null, "Request validation failed",
                errors, OffsetDateTime.now(), null);
    }

    public record ErrorItem(String field, String message) {
    }
}
