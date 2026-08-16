package com.igrejahub.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends BusinessException {
    public TenantAccessDeniedException() {
        super("Acesso não autorizado a este recurso de outra organização", "TENANT_ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }

    public TenantAccessDeniedException(String message) {
        super(message, "TENANT_ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }
}