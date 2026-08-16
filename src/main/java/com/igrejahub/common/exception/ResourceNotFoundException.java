package com.igrejahub.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s com ID %d não encontrado", resource, id), 
              "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
