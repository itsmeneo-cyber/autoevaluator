package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.domain.exception.DomainException;
import com.autoevaluator.domain.exception.InvalidRoleException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public class RestErrorMapper {

    public static ErrorResponse fromInvalidRoleException(InvalidRoleException ex, Locale locale) {

        return new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    public static ErrorResponse fromDomainException(DomainException ex, Locale locale) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }

    public static ErrorResponse asValidationError(Locale locale) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed.");
    }
}
