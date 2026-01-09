package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

/**
 * Core unit tests for {@link ErrorTicket} focusing on essential functionality
 * that complements the existing test suite.
 */
@DisplayName("Core ErrorTicket Tests")
class CoreErrorTicketTest {

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTest {
        
        @Test
        @DisplayName("Builder should support all RFC 7807 fields")
        void testRfc7807Fields() {
            var errorId = UUID.randomUUID().toString();
            var ticket = ErrorTicket.builder()
                .withErrorId(errorId)
                .withError(ErrorCodes.GeneralError)
                .withErrorGroup(ErrorCodes.GROUP)
                .withErrorDetails("Validation failed")
                .withStatusCode(400)
                .type("https://errors.example.com/validation")
                .title("Validation Error")
                .instance("/users/123")
                .build();
            
            var json = ticket.toJson();
            assertAll("RFC 7807 Problem Details",
                () -> assertEquals("https://errors.example.com/validation", json.getString("type")),
                () -> assertEquals("Validation Error", json.getString("title")),
                () -> assertEquals(400, json.getInteger("status")),
                () -> assertEquals("Validation failed", json.getString("detail")),
                () -> assertEquals("/users/123", json.getString("instance"))
            );
        }
        
        @Test
        @DisplayName("Builder should create error ticket from exception")
        void testBuildFromException() {
            var ex = new RuntimeException("Test error");
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.GeneralError)
                .withErrorGroup(ErrorCodes.GROUP)
                .withErrorDetails(ex.getMessage())
                .build();
            
            assertAll("Exception-based Error Ticket",
                () -> assertEquals(ErrorCodes.GeneralError, ticket.getError()),
                () -> assertEquals("Test error", ticket.getErrorDetails()),
                () -> assertNotNull(ticket.getErrorId())
            );
        }
    }

    @Nested
    @DisplayName("Header Generation Tests")
    class HeaderTest {
        
        @Test
        @DisplayName("OAuth errors should include WWW-Authenticate header")
        void testOAuthWwwAuthenticateHeader() {
            var ticket = ErrorTicket.builder()
                .withError(OAuthErrorCodes.InvalidToken)
                .withErrorGroup("oauth")
                .withErrorDetails("Token expired")
                .withStatusCode(401)
                .type("https://errors.example.com/auth/invalid-token")
                .build();

            var headers = ticket.getResponseHeaders();
            var wwwAuth = headers.get("WWW-Authenticate");

            assertAll("WWW-Authenticate Header",
                () -> assertNotNull(wwwAuth, "WWW-Authenticate header must be present"),
                () -> assertTrue(wwwAuth.contains("Bearer"), "Must specify Bearer auth"),
                () -> assertTrue(wwwAuth.contains("error=\"invalid_token\""), "Must include error"),
                () -> assertTrue(wwwAuth.contains("error_description=\"Token expired\""), "Must include description")
            );
        }
        
        @Test
        @DisplayName("Non-OAuth errors should have minimal headers")
        void testNonOAuthHeaders() {
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.NotFound)
                .withErrorGroup(ErrorCodes.GROUP)
                .withStatusCode(404)
                .build();

            var headers = ticket.getResponseHeaders();
            assertFalse(headers.containsKey("WWW-Authenticate"),
                "Non-OAuth errors should not have WWW-Authenticate header");
        }
    }

    @Nested
    @DisplayName("Extension Tests")
    class ExtensionTest {
        
        @Test
        @DisplayName("Should support custom extension fields")
        void testCustomExtensions() {
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.GeneralError)
                .withErrorGroup(ErrorCodes.GROUP)
                .withErrorDetails("Invalid input")
                .addExtension("field_errors", Map.of(
                    "email", "Invalid email format",
                    "age", "Must be positive number"
                ))
                .build();

            var json = ticket.toJson();
            assertAll("Custom Extensions",
                () -> assertTrue(json.containsKey("field_errors"), "Must include extension field"),
                () -> assertNotNull(json.getJsonObject("field_errors"), "Extension must be valid JSON object"),
                () -> assertEquals(2, json.getJsonObject("field_errors").size(), "Must have all error fields")
            );
        }
    }

    @Nested
    @DisplayName("toJson Serialization Tests")
    class ToJsonTest {

        @Test
        @DisplayName("toJson should serialize standard and RFC 7807 fields")
        void toJsonSerializesStandardFields() {
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.NotFound)
                .withErrorGroup(ErrorCodes.GROUP)
                .withErrorDetails("Missing resource")
                .withStatusCode(404)
                .title("Resource Missing")
                .type("https://errors.inqwise.com/not-found")
                .instance("/orders/42")
                .addExtension("traceId", "abc-123")
                .build();

            var json = ticket.toJson();

            assertAll("Serialized error fields",
                () -> assertEquals(ErrorCodes.NotFound.toString(), json.getString(ErrorTicket.Keys.CODE)),
                () -> assertEquals("Missing resource", json.getString(ErrorTicket.Keys.DETAIL)),
                () -> assertEquals(ErrorCodes.GROUP, json.getString(ErrorTicket.Keys.ERROR_GROUP)),
                () -> assertEquals(404, json.getInteger(ErrorTicket.Keys.STATUS_CODE)),
                () -> assertEquals(404, json.getInteger(ErrorTicket.Keys.STATUS)),
                () -> assertEquals("https://errors.inqwise.com/not-found", json.getString(ErrorTicket.Keys.TYPE)),
                () -> assertEquals("Resource Missing", json.getString(ErrorTicket.Keys.TITLE)),
                () -> assertEquals("/orders/42", json.getString(ErrorTicket.Keys.INSTANCE)),
                () -> assertTrue(json.containsKey(ErrorTicket.Keys.ERROR_ID), "Error id must be serialized"),
                () -> assertEquals("abc-123", json.getString("traceId"))
            );
        }

        @Test
        @DisplayName("toJson should add OAuth specific aliases")
        void toJsonAddsOAuthAliases() {
            var ticket = ErrorTicket.builder()
                .withError(OAuthErrorCodes.InvalidClient)
                .withErrorGroup("oauth")
                .withErrorDetails("Client credentials rejected")
                .type("https://errors.inqwise.com/oauth/invalid_client")
                .build();

            var json = ticket.toJson();

            assertAll("OAuth serialization",
                () -> assertEquals("invalid_client", json.getString(ErrorTicket.Keys.ERROR)),
                () -> assertEquals("Client credentials rejected", json.getString(ErrorTicket.Keys.ERROR_DESCRIPTION)),
                () -> assertEquals("https://errors.inqwise.com/oauth/invalid_client", json.getString(ErrorTicket.Keys.ERROR_URI)),
                () -> assertEquals("oauth", json.getString(ErrorTicket.Keys.ERROR_GROUP)),
                () -> assertEquals("invalid_client", json.getString(ErrorTicket.Keys.CODE))
            );
        }
    }

    @Nested
    @DisplayName("Content Type Tests")
    class ContentTypeTest {
        
        @Test
        @DisplayName("Should use application/problem+json for RFC 7807 errors")
        void testProblemJsonContentType() {
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.GeneralError)
                .withErrorGroup(ErrorCodes.GROUP)
                .type("https://errors.example.com/validation")
                .build();

            assertEquals("application/problem+json", ticket.getContentType(),
                "RFC 7807 errors should use problem+json content type");
        }

        @Test
        @DisplayName("Should use application/json for OAuth errors")
        void testOAuthContentType() {
            var ticket = ErrorTicket.builder()
                .withError(OAuthErrorCodes.InvalidToken)
                .withErrorGroup("oauth")
                .build();

            assertEquals("application/json", ticket.getContentType(),
                "OAuth errors should use json content type");
        }
    }

    @Nested
    @DisplayName("Error Comparison Tests")
    class ComparisonTest {
        
        @Test
        @DisplayName("Should correctly compare error codes")
        void testErrorCodeComparison() {
            var ticket = ErrorTicket.builder()
                .withError(ErrorCodes.NotFound)
                .withErrorGroup(ErrorCodes.GROUP)
                .build();

            assertAll("Error Code Comparisons",
                () -> assertTrue(ticket.hasError(ErrorCodes.NotFound), "Should match exact error"),
                () -> assertFalse(ticket.hasError(ErrorCodes.GeneralError), "Should not match different error"),
                () -> assertTrue(ticket.hasErrorExcept(ErrorCodes.GeneralError), "Should match when error is excluded"),
                () -> assertFalse(ticket.hasErrorExcept(ErrorCodes.NotFound), "Should not match when error is in except list")
            );
        }
    }
}
