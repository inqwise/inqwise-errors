package com.inqwise.errors;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests compliance with RFC 7807 - Problem Details for HTTP APIs.
 * 
 * @see &lt;a href="https://datatracker.ietf.org/doc/html/rfc7807"&gt;RFC 7807&lt;/a&gt;
 */
@DisplayName("RFC 7807 Compliance Tests")
class RFC7807Test {
    
    @Test
    @DisplayName("RFC 7807: Problem Details Object Format")
    void testProblemDetailsFormat() {
        // Create error with RFC 7807 fields
		var error = ErrorTicket.builder()
            .withError(TestErrorCodes.ValidationFailed)
            .withErrorGroup("validation")
            .title("Validation Failed")
            .withErrorDetails("One or more fields failed validation")
            .withStatusCode(400)
            .instance("/users/123/profile")
            .type(URI.create("https://errors.inqwise.com/validation-error").toString())
            .build();

        // Validate JSON structure matches RFC 7807 format
        var json = error.toJson();
        assertNotNull(json);
        assertAll("RFC 7807 Required Fields",
            () -> assertTrue(json.containsKey("type"), "Problem Details must have 'type' field"),
            () -> assertTrue(json.containsKey("title"), "Problem Details must have 'title' field"),
            () -> assertTrue(json.containsKey("status"), "Problem Details must have 'status' field"),
            () -> assertTrue(json.containsKey("detail"), "Problem Details must have 'detail' field"),
            () -> assertTrue(json.containsKey("instance"), "Problem Details must have 'instance' field")
        );
    }

    @Test
    @DisplayName("RFC 7807: Content Type Headers")
    void testProblemDetailsContentType() {
		var error = ErrorTicket.builder()
            .withError(TestErrorCodes.NotFound)
            .withErrorGroup("http")
            .title("Resource Not Found")
            .withErrorDetails("The requested resource does not exist")
            .withStatusCode(404)
            .type("https://errors.inqwise.com/not-found")
            .build();

        // Verify correct content type is used
        assertEquals(
            "application/problem+json",
            error.getContentType(),
            "RFC 7807 requires application/problem+json content type"
        );
    }

    @Test
    @DisplayName("RFC 7807: Extension Members")
    void testProblemDetailsExtensions() {
        // Create error with custom extension fields
		var error = ErrorTicket.builder()
            .withError(TestErrorCodes.RateLimitExceeded)
            .withErrorGroup("throttling")
            .title("Rate Limit Exceeded")
            .withErrorDetails("API rate limit has been exceeded")
            .withStatusCode(429)
            .addExtension("requestLimit", 100)
            .addExtension("requestCount", 150)
            .addExtension("retryAfter", 30)
            .build();

        var json = error.toJson();
        
        // Verify extension members
        assertAll("RFC 7807 Extension Members",
            () -> assertEquals(100, json.getInteger("requestLimit")),
            () -> assertEquals(150, json.getInteger("requestCount")),
            () -> assertEquals(30, json.getInteger("retryAfter")),
            () -> assertTrue(json.containsKey("code"), "Should include error code extension")
        );
    }

    @Test
    @DisplayName("RFC 7807: Internationalization Support")
    void testProblemDetailsI18n() {
		var error = ErrorTicket.builder()
            .withError(TestErrorCodes.InvalidInput)
            .withErrorGroup("validation")
            .title("Invalid Input")
            .withErrorDetails("The provided input is invalid")
            .withStatusCode(400)
            .addExtension("supportedLanguages", new String[]{"en", "es", "fr"})
            .build();
            
        // Verify i18n capabilities
        assertTrue(error.supportsLocalization(), 
            "Error handler should support internationalization");
    }
}