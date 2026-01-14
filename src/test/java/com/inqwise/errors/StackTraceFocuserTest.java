package com.inqwise.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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

	@Test
	void defaultInstanceReturnsCachedInstance() {
		var first = StackTraceFocuser.defaultInstance();
		var second = StackTraceFocuser.defaultInstance();

		assertSame(first, second);
	}

	@Test
	void toPredicateFromClassPatternsKeepsNonMatchingFrames() throws Exception {
		Predicate<StackTraceElement> predicate = StackTraceFocuser.toPredicate(
			List.of(Pattern.compile("^com\\.drop\\.")));

		var dropped = new StackTraceElement("com.drop.Target", "run", "Target.java", 1);
		var kept = new StackTraceElement("com.keep.Target", "run", "Target.java", 2);

		assertAll(
			() -> assertFalse(predicate.test(dropped)),
			() -> assertTrue(predicate.test(kept))
		);
	}

	@Test
	void andIgnoreClassNamesCombinesPatterns() {
		var base = StackTraceFocuser.ignoreClassNames(List.of(Pattern.compile("^com\\.dropA\\.")));
		var combined = base.andIgnoreClassNames(List.of(Pattern.compile("^com\\.dropB\\.")));

		var original = new RuntimeException("boom");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("com.dropA.Type", "run", "Type.java", 1),
			new StackTraceElement("com.dropB.Type", "run", "Type.java", 2)
		});

		var focused = combined.apply(original);

		assertEquals(0, focused.getStackTrace().length);
	}

	@Test
	void varargsConstructorCombinesPredicates() {
		Predicate<StackTraceElement> classIgnore = StackTraceFocuser.ignoreClassName(Pattern.compile("^com\\.drop\\."));
		Predicate<StackTraceElement> methodIgnore = StackTraceFocuser.ignoreMethodName(Pattern.compile("^skip$"));
		var focuser = new StackTraceFocuser(classIgnore, methodIgnore);

		var original = new RuntimeException("boom");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("com.drop.Type", "run", "Type.java", 1),
			new StackTraceElement("com.keep.Type", "skip", "Type.java", 2),
			new StackTraceElement("com.keep.Type", "run", "Type.java", 3)
		});

		var focused = focuser.apply(original);

		assertAll(
			() -> assertEquals(1, focused.getStackTrace().length),
			() -> assertEquals("com.keep.Type", focused.getStackTrace()[0].getClassName())
		);
	}

	@Test
	void applyTypedReturnsSameThrowableType() {
		var original = new CustomException("custom");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("java.lang.Thread", "run", "Thread.java", 1),
			new StackTraceElement("com.keep.Type", "run", "Type.java", 2)
		});

		var focuser = StackTraceFocuser.builder().skipDefaultPatterns().addClass("^java\\.").build();
		var focused = focuser.applyTyped(original);

		assertAll(
			() -> assertTrue(focused instanceof CustomException),
			() -> assertEquals(1, focused.getStackTrace().length),
			() -> assertEquals("com.keep.Type", focused.getStackTrace()[0].getClassName())
		);
	}

	@Test
	void instantiateLikeUsesAvailableConstructors() throws Exception {
		var focuser = StackTraceFocuser.builder().skipDefaultPatterns().build();
		var cause = new IllegalStateException("cause");

		var msgCause = focuser.instantiateLike(new MsgCauseException("msg"), cause);
		var stringOnly = focuser.instantiateLike(new StringOnlyException("text"), cause);
		var causeOnly = focuser.instantiateLike(new CauseOnlyException(cause), cause);
		var noArg = focuser.instantiateLike(new NoArgException(), cause);

		assertAll(
			() -> assertEquals("msg", msgCause.getMessage()),
			() -> assertSame(cause, msgCause.getCause()),
			() -> assertEquals("text", stringOnly.getMessage()),
			() -> assertSame(cause, stringOnly.getCause()),
			() -> assertSame(cause, causeOnly.getCause()),
			() -> assertSame(cause, noArg.getCause())
		);
	}

	@Test
	void constructReturnsNullWhenCtorMissing() throws Exception {
		var created = StackTraceFocuser.construct(StringOnlyException.class, new Class<?>[] { String.class },
			new Object[] { "ok" });
		var missing = StackTraceFocuser.construct(StringOnlyException.class, new Class<?>[] { Integer.class },
			new Object[] { 1 });

		assertAll(
			() -> assertNotNull(created),
			() -> assertNull(missing)
		);
	}

	@Test
	void builderSupportsAllAddMethods() {
		var original = new RuntimeException("boom");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("com.drop.ByPattern", "stay", "Keep.java", 1),
			new StackTraceElement("com.drop.ByCollection", "stay", "Keep.java", 2),
			new StackTraceElement("com.drop.ByRegex1", "stay", "Keep.java", 3),
			new StackTraceElement("com.drop.ByRegex2", "stay", "Keep.java", 4),
			new StackTraceElement("com.keep.Visible", "dropByPattern", "Keep.java", 5),
			new StackTraceElement("com.keep.Visible", "dropByRegex1", "Keep.java", 6),
			new StackTraceElement("com.keep.Visible", "dropByRegex2", "Keep.java", 7),
			new StackTraceElement("com.keep.Visible", "dropByCollection", "Keep.java", 8),
			new StackTraceElement("com.keep.Visible", "stay", "DropByPattern.java", 9),
			new StackTraceElement("com.keep.Visible", "stay", "DropByRegex1.java", 10),
			new StackTraceElement("com.keep.Visible", "stay", "DropByRegex2.java", 11),
			new StackTraceElement("com.keep.Visible", "stay", "DropByCollection.java", 12),
			new StackTraceElement("com.keep.Visible", "stay", "Keep.java", 13)
		});

		var focuser = StackTraceFocuser.builder().skipDefaultPatterns()
			.addClass(Pattern.compile("^com\\.drop\\.ByPattern$"))
			.addClasses(List.of(Pattern.compile("^com\\.drop\\.ByCollection$")))
			.addClass("^com\\.drop\\.ByRegex1$", "^com\\.drop\\.ByRegex2$")
			.addMethod(Pattern.compile("^dropByPattern$"))
			.addMethod("^dropByRegex1$", "^dropByRegex2$")
			.addMethods(List.of(Pattern.compile("^dropByCollection$")))
			.addFile(Pattern.compile("^DropByPattern\\.java$"))
			.addFile("^DropByRegex1\\.java$", "^DropByRegex2\\.java$")
			.addFiles(List.of(Pattern.compile("^DropByCollection\\.java$")))
			.build();

		var focused = focuser.apply(original);

		assertAll(
			() -> assertEquals(1, focused.getStackTrace().length),
			() -> assertEquals("Keep.java", focused.getStackTrace()[0].getFileName())
		);
	}

	@Test
	void buildHonorsSkipDefaultPatterns() {
		var original = new RuntimeException("boom");
		original.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("java.lang.String", "valueOf", "String.java", 1),
			new StackTraceElement("com.keep.Type", "run", "Type.java", 2)
		});

		var withDefaults = StackTraceFocuser.builder().build().apply(original);
		var withoutDefaults = StackTraceFocuser.builder().skipDefaultPatterns().build().apply(original);

		assertAll(
			() -> assertEquals(1, withDefaults.getStackTrace().length),
			() -> assertEquals("com.keep.Type", withDefaults.getStackTrace()[0].getClassName()),
			() -> assertEquals(2, withoutDefaults.getStackTrace().length)
		);
	}

	static class CustomException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		CustomException(String message) {
			super(message);
		}
	}

	static class MsgCauseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		MsgCauseException(String message) {
			super(message);
		}

		@SuppressWarnings("unused")
		MsgCauseException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	static class StringOnlyException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		StringOnlyException(String message) {
			super(message);
		}
	}

	static class CauseOnlyException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		CauseOnlyException(Throwable cause) {
			super(cause);
		}
	}

	static class NoArgException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		NoArgException() {
			super();
		}
	}
}
