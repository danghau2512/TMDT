package com.example.demo.exception;

public final class AccountUnavailableException extends RuntimeException {
    public enum Reason { DATABASE_NOT_CONFIGURED, DATABASE_CONFIGURATION_INVALID, SERVICE_NOT_INITIALIZED, DATABASE_OPERATION_FAILED }
    private final Reason reason;
    public AccountUnavailableException() { this(Reason.DATABASE_OPERATION_FAILED); }
    public AccountUnavailableException(Reason reason) {
        super("Chức năng tài khoản tạm thời không khả dụng. Vui lòng thử lại sau.");
        this.reason = reason;
    }
    /** Mã cố định cho log server, không chứa SQL/cause/giá trị cấu hình. */
    public Reason reason() { return reason; }
}
