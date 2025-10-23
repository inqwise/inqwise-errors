package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class ErrorInfrastructureTest {

    @Test
    void bugPrefixesMessagesAndSupportsCause() {
        var cause = new IllegalArgumentException("boom");
        var bug = new Bug(cause, "Failure {}", 42);

        assertAll(
            () -> assertTrue(bug.getMessage().startsWith("BUG: Failure 42")),
            () -> assertSame(cause, bug.getCause())
        );
    }

    @Test
    void bugFormattingConstructorDelegatesToFullConstructor() {
        var bug = new Bug("Missing {}", "bits");

        assertTrue(bug.getMessage().contains("Missing bits"));
    }

    @Test
    void notImplementedExceptionStoresOptionalCode() {
        var ex = new NotImplementedException("missing", "ERR");

        assertEquals("ERR", ex.getCode());
    }

    @Test
    void notFoundExceptionFormatsMessageAndToErrorTicket() {
        var ex = new NotFoundException("User {}", 12);

        assertTrue(ex.getMessage().contains("User 12"));

        var ticket = ex.toErrorTicket();

        assertEquals(ErrorCodes.NotFound, ticket.getError());
    }

    @Test
    void exceptionNormalizerUnboxesAndFocusesStack() {
        var inner = new IllegalStateException("inner");
        inner.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("java.lang.Thread", "run", "Thread.java", 1),
            new StackTraceElement("com.acme.App", "execute", "App.java", 2)
        });

        var wrapped = new CompletionException(inner);

        var normalizer = ExceptionNormalizer.notmalizer();
        var normalized = normalizer.normalize(wrapped);

        assertAll(
            () -> assertSame(inner, normalized),
            () -> assertEquals(1, normalized.getStackTrace().length),
            () -> assertEquals("com.acme.App", normalized.getStackTrace()[0].getClassName()),
            () -> assertNotNull(normalizer.stackTraceFocuser())
        );
    }

    @Test
    void errorCodeProvidersLoadFromServiceDiscovery() {
        var provider = ErrorCodeProviders.get(ErrorCodes.GROUP);

        assertNotNull(provider);
        assertEquals(ErrorCodes.GeneralError, provider.valueOf("GeneralError"));

        var providers = ErrorCodeProviders.getAll();

        assertFalse(providers.isEmpty());
    }

    @Test
    void errorCodeProvidersReturnNullWhenGroupMissing() {
        assertNull(ErrorCodeProviders.get("missing"));
    }

    @Test
    void errorCodesProviderResolvesEnumValues() {
        var provider = new ErrorCodesProvider();

        assertAll(
            () -> assertEquals(ErrorCodes.GROUP, provider.group()),
            () -> assertEquals(ErrorCodes.GeneralError, provider.valueOf("GeneralError")),
            () -> assertNull(provider.valueOf("Unknown"))
        );
    }

    @Test
    void undefinedErrorCodeCreatedForUnknownValues() {
        var json = new JsonObject()
            .put(ErrorTicket.Keys.CODE, "MISSING")
            .put(ErrorTicket.Keys.ERROR_GROUP, ErrorCodes.GROUP);

        var ticket = new ErrorTicket(json);

        assertTrue(ticket.getError() instanceof ErrorTicket.UndefinedErrorCode);
        var undefined = (ErrorTicket.UndefinedErrorCode) ticket.getError();

        assertAll(
            () -> assertEquals("MISSING", undefined.getName()),
            () -> assertNull(undefined.group()),
            () -> assertEquals("MISSING", undefined.toString())
        );
    }

    @Test
    void errorTicketKeysAccessible() {
        var keys = new ErrorTicket.Keys();

        assertAll(
            () -> assertNotNull(keys),
            () -> assertEquals("code", ErrorTicket.Keys.CODE),
            () -> assertEquals("status", ErrorTicket.Keys.STATUS)
        );
    }
}
