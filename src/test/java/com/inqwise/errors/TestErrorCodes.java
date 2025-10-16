package com.inqwise.errors;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Test error codes used in RFC compliance tests.
 */
public enum TestErrorCodes implements ErrorCode {
    ValidationFailed("validation_failed"),
    NotFound("not_found"),
    RateLimitExceeded("rate_limit_exceeded"),
    InvalidInput("invalid_input");

    private final String code;

    TestErrorCodes(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }

    @Override
    public String group() {
        return null; // Not used in tests
    }
}