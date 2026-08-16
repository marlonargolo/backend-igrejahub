package com.igrejahub.common.exception;

import org.springframework.http.HttpStatus;

public class AccountingAccessDeniedException extends BusinessException {
    public AccountingAccessDeniedException() {
        super("Acesso não autorizado ao módulo financeiro/contábil", "ACCOUNTING_ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }

    public AccountingAccessDeniedException(String message) {
        super(message, "ACCOUNTING_ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }
}