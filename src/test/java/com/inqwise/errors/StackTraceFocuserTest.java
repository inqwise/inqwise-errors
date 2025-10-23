package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class StackTraceFocuserTest {

    @Test
    void ignoreJavaClassesRemovesJdkFrames() {
        var cause = new IllegalStateException("cause");
        cause.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("java.lang.Thread", "run", "Thread.java", 10),
            new StackTraceElement("com.inqwise.errors.Sample", "call", "Sample.java", 20)
        });

        var ex = new RuntimeException("boom", cause);
        ex.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("java.util.concurrent.FutureTask", "run", "FutureTask.java", 50),
            new StackTraceElement("com.inqwise.errors.Main", "execute", "Main.java", 60)
        });

        var focuser = StackTraceFocuser.<Throwable>ignoreJavaClasses();

        focuser.apply(ex);

        assertAll(
            () -> assertEquals(1, ex.getStackTrace().length),
            () -> assertEquals("com.inqwise.errors.Main", ex.getStackTrace()[0].getClassName()),
            () -> assertEquals(1, cause.getStackTrace().length),
            () -> assertEquals("com.inqwise.errors.Sample", cause.getStackTrace()[0].getClassName())
        );
    }

    @Test
    void additionalClassIgnoresCombineWithExisting() {
        var ex = new RuntimeException("combined");
        ex.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.inqwise.internal.Helper", "call", "Helper.java", 1),
            new StackTraceElement("org.example.Visible", "run", "Visible.java", 2)
        });

        var base = StackTraceFocuser.<Throwable>ignoreJavaClasses();
        var extended = base.andIgnoreClassNames(List.of(Pattern.compile("^com\\.inqwise\\.internal")));

        extended.apply(ex);

        assertEquals(0, ex.getStackTrace().length);
    }

    @Test
    void helperPredicatesMatchFields() {
        var frame = new StackTraceElement("com.inqwise.Test", "perform", "Test.java", 42);

        assertAll(
            () -> assertTrue(StackTraceFocuser.ignoreClassName(Pattern.compile("inqwise")).test(frame)),
            () -> assertTrue(StackTraceFocuser.ignoreMethodName(Pattern.compile("perform")).test(frame)),
            () -> assertTrue(StackTraceFocuser.ignoreFileName(Pattern.compile("Test\\.java")).test(frame)),
            () -> assertTrue(StackTraceFocuser.ignoreLineNumber(Pattern.compile("42")).test(frame))
        );
    }
}
