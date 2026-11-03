package com.cloudcostai.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue), HttpStatus.CONFLICT);
    }
}
