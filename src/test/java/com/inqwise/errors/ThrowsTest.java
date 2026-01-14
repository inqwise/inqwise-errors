package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class ThrowsTest {

    @Test
    void propagateWrapsCheckedExceptions() {
        var checked = new Exception("boom");

        var ex = assertThrows(RuntimeException.class, () -> Throws.propagate(checked));

        assertSame(checked, ex.getCause());
    }

    @Test
    void notImplementedProvidesDefaultMessage() {
        var ex = Throws.notImplemented();

        assertEquals("not implemented", ex.getMessage());
    }

    @Test
    void notImplementedUsesCustomMessage() {
        var ex = Throws.notImplemented("custom");

        assertEquals("custom", ex.getMessage());
    }

    @Test
    void unboxSingleClassPeelsWrapper() {
        var inner = new java.io.IOException("inner");
        var wrapper = new RuntimeException(inner);

        var result = Throws.unbox(wrapper, RuntimeException.class);

        assertSame(inner, result);
    }

    @Test
    void unboxTwoClassesPeelsUntilDifferentType() {
        var deepest = new java.io.IOException("deep");
        var mid = new CompletionException(deepest);
        var outer = new RuntimeException(mid);

        var result = Throws.unbox(outer, RuntimeException.class, CompletionException.class);

        assertSame(deepest, result);
    }

    @Test
    void unboxVarargsPeelsThroughConfiguredTypes() {
        var deepest = new java.io.IOException("core");
        var second = new CompletionException(deepest);
        var third = new IllegalStateException(second);
        var outer = new RuntimeException(third);

        var result = Throws.unbox(outer, RuntimeException.class, IllegalStateException.class,
            CompletionException.class);

        assertSame(deepest, result);
    }

    @Test
    void notFoundFormatsMessage() {
        var ex = Throws.notFound("user {} missing", 12);

        assertAll(
            () -> assertTrue(ex instanceof NotFoundException),
            () -> assertEquals("Item Not Found:user 12 missing", ex.getMessage())
        );
    }
}
