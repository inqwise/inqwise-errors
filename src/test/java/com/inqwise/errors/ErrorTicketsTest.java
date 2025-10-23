package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ErrorTicketsTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        void notFoundFromStringUsesMessage() {
            var ticket = ErrorTickets.notFound("missing item");

            assertAll(
                () -> assertEquals(ErrorCodes.NotFound, ticket.getError()),
                () -> assertEquals("missing item", ticket.getErrorDetails())
            );
        }

        @Test
        void notFoundFromThrowableUsesRootCause() {
            var deep = new IllegalStateException("deep cause");
            var wrapped = new RuntimeException("wrap", deep);

            var ticket = ErrorTickets.notFound(wrapped);

            assertEquals("deep cause", ticket.getErrorDetails());
        }

        @Test
        void generalCreatesGeneralErrorTicket() {
            var ticket = ErrorTickets.general("boom");

            assertEquals(ErrorCodes.GeneralError, ticket.getError());
        }

        @Test
        void noLoggedInSetsUnauthorizedStatus() {
            var ticket = ErrorTickets.noLoggedIn();

            assertAll(
                () -> assertEquals(ErrorCodes.NotLoggedIn, ticket.getError()),
                () -> assertEquals(401, ticket.getStatus())
            );
        }

        @Test
        void notImplementedFormatsMessage() {
            var ticket = ErrorTickets.notImplemented("Feature {}", "X");

            assertAll(
                () -> assertEquals(ErrorCodes.NotImplemented, ticket.getError()),
                () -> assertTrue(ticket.getErrorDetails().contains("Feature X"))
            );
        }

        @Test
        void notImplementedShortcutUsesDefaultMessage() {
            var ticket = ErrorTickets.notImplemented("Fallback");

            assertEquals("Fallback", ticket.getErrorDetails());
        }
    }

    @Nested
    @DisplayName("Null Checks")
    class NullChecks {

        @Test
        void checkAnyNotNullThrowsWhenAllNull() {
            var values = new java.util.ArrayList<Object>();
            values.add(null);
            values.add(null);

            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkAnyNotNull(values, "missing"));

            assertAll(
                () -> assertEquals(ErrorCodes.ArgumentNull, ex.getError()),
                () -> assertEquals("missing", ex.getErrorDetails())
            );
        }

        @Test
        void checkAnyNotNullAcceptsNonNullValue() {
            assertDoesNotThrow(() -> ErrorTickets.checkAnyNotNull(new Object[]{"value"}, "ignored"));
        }

        @Test
        void checkAnyNotNullWithConsumerAllowsCustomization() {
            var values = new java.util.ArrayList<Object>();
            values.add(null);

            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkAnyNotNull(values, builder -> builder
                    .withErrorDetails("custom")
                    .withErrorGroup("test")));

            assertAll(
                () -> assertEquals(ErrorCodes.ArgumentNull, ex.getError()),
                () -> assertEquals("custom", ex.getErrorDetails()),
                () -> assertEquals("test", ex.getErrorGroup())
            );
        }

        @Test
        void checkAllNotNullThrowsWhenAnyNull() {
            var values = new java.util.ArrayList<Object>();
            values.add("value");
            values.add(null);

            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkAllNotNull(values, "invalid"));

            assertEquals("invalid", ex.getErrorDetails());
        }

        @Test
        void checkAllNotNullWithConsumerAllowsCustomization() {
            var values = new java.util.ArrayList<Object>();
            values.add(null);

            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkAllNotNull(values, builder -> builder
                    .withErrorDetails("fail")
                    .withStatusCode(409)));

            assertAll(
                () -> assertEquals(ErrorCodes.ArgumentNull, ex.getError()),
                () -> assertEquals("fail", ex.getErrorDetails()),
                () -> assertEquals(409, ex.getStatus())
            );
        }

        @Test
        void checkAllNotNullArrayAcceptsNonNullValues() {
            assertDoesNotThrow(() -> ErrorTickets.checkAllNotNull(new Object[]{"value"}, builder -> {}));
        }

        @Test
        void checkNotNullReturnsReference() {
            var value = UUID.randomUUID();

            assertSame(value, ErrorTickets.checkNotNull(value, "ok"));
        }

        @Test
        void checkNotNullThrowsWithCustomErrorCode() {
            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkNotNull(null, "required", ErrorCodes.NotFound));

            assertAll(
                () -> assertEquals(ErrorCodes.NotFound, ex.getError()),
                () -> assertEquals(ErrorCodes.GROUP, ex.getErrorGroup()),
                () -> assertEquals("required", ex.getErrorDetails())
            );
        }

        @Test
        void checkNotNullWithConsumerAllowsCustomization() {
            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkNotNull(null, builder -> builder.withStatusCode(400)));

            assertEquals(400, ex.getStatus());
        }

        @Test
        void thenCheckNotNullDelegatesToCheckNotNull() {
            var fn = ErrorTickets.thenCheckNotNull("empty");

            assertThrows(ErrorTicket.class, () -> fn.apply(null));
            assertEquals("value", fn.apply("value"));
        }

        @Test
        void thenCheckNotNullConsumerDelegates() {
            var fn = ErrorTickets.thenCheckNotNull(builder -> builder.withStatusCode(418));

            var ex = assertThrows(ErrorTicket.class, () -> fn.apply(null));
            assertEquals(418, ex.getStatus());
        }
    }

    @Nested
    @DisplayName("Argument Checks")
    class ArgumentChecks {

        @Test
        void checkArgumentThrowsWithDetails() {
            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkArgument(false, "invalid"));

            assertAll(
                () -> assertEquals(ErrorCodes.ArgumentWrong, ex.getError()),
                () -> assertEquals("invalid", ex.getErrorDetails())
            );
        }

        @Test
        void checkArgumentThrowsWithCustomErrorCode() {
            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkArgument(false, "bad", ErrorCodes.NotPermitted));

            assertAll(
                () -> assertEquals(ErrorCodes.NotPermitted, ex.getError()),
                () -> assertEquals(ErrorCodes.GROUP, ex.getErrorGroup())
            );
        }

        @Test
        void checkArgumentConsumerAllowsCustomisation() {
            var ex = assertThrows(ErrorTicket.class,
                () -> ErrorTickets.checkArgument(false, builder -> builder.withErrorDetails("oops")));

            assertAll(
                () -> assertEquals(ErrorCodes.ArgumentWrong, ex.getError()),
                () -> assertEquals("oops", ex.getErrorDetails())
            );
        }

        @Test
        void checkArgumentPassesWhenExpressionTrue() {
            assertDoesNotThrow(() -> ErrorTickets.checkArgument(true, "ignored"));
        }
    }
}
