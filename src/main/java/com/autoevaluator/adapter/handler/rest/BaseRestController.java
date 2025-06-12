package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.exception.DomainException;
import com.autoevaluator.domain.exception.InvalidRoleException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import static com.autoevaluator.adapter.handler.rest.RestErrorMapper.*;

public abstract class BaseRestController {

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleException ex, WebRequest request) {
        return ResponseEntity.status(403).body(fromInvalidRoleException(ex, request.getLocale()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainError(DomainException ex, WebRequest request) {
        return ResponseEntity.status(500).body(fromDomainException(ex, request.getLocale()));
    }

    @ExceptionHandler({ ConstraintViolationException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponse> handleValidationError(Exception ex, WebRequest request) {
        return ResponseEntity.badRequest().body(asValidationError(request.getLocale()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred."));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }


}
