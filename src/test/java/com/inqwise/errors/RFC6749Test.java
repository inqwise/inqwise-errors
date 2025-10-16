package com.inqwise.errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests compliance with RFC 6749 - The OAuth 2.0 Authorization Framework.
 * Specifically focuses on section 5.2 "Error Response".
 * 
 * @see &lt;a href="https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"&gt;RFC 6749 Section 5.2&lt;/a&gt;
 */
@DisplayName("RFC 6749 Compliance Tests")
class RFC6749Test {
    
    @Test
    @DisplayName("RFC 6749: Basic Error Response Format")
    void testOAuthErrorResponseFormat() {
		var error = ErrorTicket.builder()
            .withError(OAuthErrorCodes.InvalidRequest)
            .withErrorGroup("oauth")
            .withErrorDetails("The request is missing a required parameter")
            .build();

        var json = error.toJson();
        assertNotNull(json);
        assertAll("OAuth Error Response Format",
            () -> assertTrue(json.containsKey("error"), "Must have 'error' field"),
            () -> assertTrue(json.containsKey("error_description"), "Should have 'error_description' field"),
            () -> assertEquals("invalid_request", json.getString("error"))
        );
    }

    @ParameterizedTest(name = "OAuth Error Code: {0}")
    @DisplayName("RFC 6749: Standard Error Codes")
    @CsvSource({
        "invalid_request, 400",
        "unauthorized_client, 401",
        "access_denied, 403",
        "unsupported_response_type, 400",
        "invalid_scope, 400",
        "server_error, 500",
        "temporarily_unavailable, 503"
    })
    void testOAuthStandardErrorCodes(String code, int expectedStatus) {
		var error = ErrorTicket.builder()
            .withError(OAuthErrorCodes.fromString(code))
            .withErrorGroup("oauth")
            .withStatusCode(expectedStatus)
            .build();

        var json = error.toJson();
        assertAll("OAuth Standard Error",
            () -> assertEquals(code, json.getString("error")),
            () -> assertEquals(expectedStatus, error.getStatus())
        );
    }

    @Test
    @DisplayName("RFC 6749: Error URI Support")
    void testOAuthErrorUri() {
		var error = ErrorTicket.builder()
            .withError(OAuthErrorCodes.InvalidClient)
            .withErrorGroup("oauth")
            .withErrorDetails("Client authentication failed")
            .type("https://errors.inqwise.com/oauth/invalid-client")
            .build();

        var json = error.toJson();
        assertTrue(json.containsKey("error_uri"), 
            "OAuth errors should support error_uri field");
    }

    @Test
    @DisplayName("RFC 6749: WWW-Authenticate Header")
    void testOAuthWwwAuthenticateHeader() {
		var error = ErrorTicket.builder()
            .withError(OAuthErrorCodes.InvalidToken)
            .withErrorGroup("oauth")
            .withErrorDetails("The access token provided is expired")
            .withStatusCode(401)
            .build();

        var headers = error.getResponseHeaders();
        assertTrue(headers.containsKey("WWW-Authenticate"), 
            "401 responses must include WWW-Authenticate header");
        
        var wwwAuth = headers.get("WWW-Authenticate");
        assertTrue(wwwAuth.contains("Bearer"), 
            "WWW-Authenticate header should specify Bearer authentication");
        assertTrue(wwwAuth.contains("error=\"invalid_token\""), 
            "WWW-Authenticate header should include error code");
    }
}