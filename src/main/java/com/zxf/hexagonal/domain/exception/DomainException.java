package com.zxf.hexagonal.domain.exception;

/**
 * 领域异常公共基类：携带稳定错误码（客户端契约），不含传输层语义（HTTP 状态由入站适配器映射）。
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 包装底层技术异常时使用（适配器翻译），必须保留 cause */
    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
