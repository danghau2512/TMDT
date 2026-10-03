package com.example.demo.config;

/** Chỉ chứa tên khóa/đường dẫn và hướng dẫn; không chứa giá trị hoặc cause có credential. */
public final class ConfigurationException extends IllegalArgumentException {
    public ConfigurationException(String message) {
        super(message);
    }
}
