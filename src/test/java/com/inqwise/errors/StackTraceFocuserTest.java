package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StackTraceFocuserTest {

	@Test
	void ignoreJavaClassesReturnsTrimmedCloneWithoutMutatingOriginal() {
		var cause = new IllegalStateException("cause");
		cause.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("java.lang.Thread", "run", "Thread.java", 10),
			new StackTraceElement("com.inqwise.errors.Sample", "call", "Sample.java", 20)
		});

		var suppressed = new IllegalArgumentException("suppressed");
		suppressed.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("java.util.Objects", "requireNonNull", "Objects.java", 30),
			new StackTraceElement("com.inqwise.errors.Helper", "visible", "Helper.java", 40)
		});

		var original = new RuntimeException("boom", cause);
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("java.util.concurrent.FutureTask", "run", "FutureTask.java", 50),
			new StackTraceElement("com.inqwise.errors.Main", "execute", "Main.java", 60)
		});
		original.addSuppressed(suppressed);

		var focuser = StackTraceFocuser.ignoreJavaClasses();
		var focused = focuser.apply(original);

		assertAll(
			() -> assertNotSame(original, focused),
			() -> assertEquals("boom", focused.getMessage()),
			() -> assertEquals(2, original.getStackTrace().length),
			() -> assertEquals(1, focused.getStackTrace().length),
			() -> assertEquals("com.inqwise.errors.Main", focused.getStackTrace()[0].getClassName()),
			() -> assertNotSame(original.getCause(), focused.getCause()),
			() -> assertEquals(1, focused.getCause().getStackTrace().length),
			() -> assertEquals("com.inqwise.errors.Sample",
				focused.getCause().getStackTrace()[0].getClassName()),
			() -> assertEquals(2, original.getSuppressed()[0].getStackTrace().length),
			() -> assertEquals(1, focused.getSuppressed().length),
			() -> assertEquals("com.inqwise.errors.Helper",
				focused.getSuppressed()[0].getStackTrace()[0].getClassName())
		);
	}

	@Test
	void builderAllowsFilteringByAllPatternTypes() {
		var original = new IllegalArgumentException("multi");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("drop.Hidden", "keep", "Keep.java", 1),
			new StackTraceElement("keep.Visible", "discardMethod", "Keep.java", 2),
			new StackTraceElement("keep.Visible", "stay", "RemoveFile.java", 3),
			new StackTraceElement("keep.Visible", "stay", "Keep.java", 5)
		});

		var focuser = StackTraceFocuser.builder().skipDefaultPatterns().addClass("^drop\\.")
			.addMethod("^discardMethod$").addFile("RemoveFile\\.java").build();

		var focused = focuser.apply(original);

		assertAll(
			() -> assertEquals(4, original.getStackTrace().length),
			() -> assertEquals(1, focused.getStackTrace().length),
			() -> assertEquals("keep.Visible", focused.getStackTrace()[0].getClassName()),
			() -> assertEquals("stay", focused.getStackTrace()[0].getMethodName()),
			() -> assertEquals(5, focused.getStackTrace()[0].getLineNumber())
		);
	}
}
