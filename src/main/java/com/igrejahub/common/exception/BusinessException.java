package com.igrejahub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public BusinessException(String message) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST, null);
    }

    public BusinessException(String message, String code) {
        this(message, code, HttpStatus.BAD_REQUEST, null);
    }

    public BusinessException(String message, String code, HttpStatus status) {
        this(message, code, status, null);
    }

    public BusinessException(String message, String code, HttpStatus status, 
                             Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
}
