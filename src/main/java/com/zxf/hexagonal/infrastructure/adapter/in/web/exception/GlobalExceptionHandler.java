package com.zxf.hexagonal.infrastructure.adapter.in.web.exception;

import com.zxf.hexagonal.domain.exception.DomainException;
import com.zxf.hexagonal.domain.exception.UserNotFoundException;
import com.zxf.hexagonal.infrastructure.adapter.in.web.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理（入站适配器边界）：领域异常在此映射为 HTTP 响应；
 * 兜底 500 固定文案绝不回显 ex.getMessage()（信息泄露防护）。
 * 映射矩阵见 exception-handling.md §6.2。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 领域异常 → HTTP（逐异常声明，传输语义只在边界层）──

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getUserId());
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    /** 冲突类领域异常共用 409（邮箱占用 / 非法状态转换 / 乐观锁冲突）。 */
    @ExceptionHandler({com.zxf.hexagonal.domain.exception.EmailAlreadyExistsException.class,
            com.zxf.hexagonal.domain.exception.UserAlreadyInactiveException.class,
            com.zxf.hexagonal.domain.exception.UserVersionConflictException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(DomainException ex) {
        log.warn("Domain conflict [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return respond(HttpStatus.CONFLICT, ex);
    }

    // ── Spring 内置异常（SF 6.1+ / SB4 行为）──

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.ErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiResponse.ErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.validationError(errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex) {
        log.warn("Handler method validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Malformed request body"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch on parameter [{}]: {}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Invalid parameter: " + ex.getName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        log.warn("No matching route");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("BAD_REQUEST", "Method not allowed"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // 唯一键冲突等：通用冲突码，禁止映射为实体特定错误码（实体语义应在 Service 层先校验）
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT", "Data conflict"));
    }

    // ── 兜底：固定文案，不回显内部信息 ──

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Internal server error"));
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, DomainException ex) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }
}
