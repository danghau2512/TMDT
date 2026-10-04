package com.example.demo.exception;

import java.util.Map;

public final class FormException extends RuntimeException {
    private final Map<String, String> errors;
    public FormException(Map<String, String> errors) {
        super("Biểu mẫu chưa hợp lệ.");
        this.errors = Map.copyOf(errors);
    }
    public Map<String, String> errors() { return errors; }
}
