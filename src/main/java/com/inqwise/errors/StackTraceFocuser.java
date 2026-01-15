package com.inqwise.errors;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

/**
 * Utility that trims exception stack traces by removing frames matched by configurable predicates.
 */
public final class StackTraceFocuser implements Function<Throwable, Throwable> {
	private static final List<Pattern> DEFAULT_JAVA_CLASS_PATTERNS = asList(compile("^java\\."),
		compile("^javax\\."), compile("^sun\\."), compile("^com\\.sun\\."));
	private static volatile StackTraceFocuser defaultInstance;
	private final Predicate<StackTraceElement> ignore;

	/**
	 * Creates a new {@code StackTraceFocuser} that ignores stack frames whose class names match the
	 * supplied patterns.
	 *
	 * @param classNameIgnores the patterns to ignore, never {@code null}
	 * @return a new {@code StackTraceFocuser}, never {@code null}
	 */
	public static StackTraceFocuser ignoreClassNames(final Collection<Pattern> classNameIgnores) {
		return new StackTraceFocuser(toPredicates(classNameIgnores));
	}

	/**
	 * Returns a lazily constructed {@code StackTraceFocuser} that ignores JDK stack frames. The
	 * instance is created on the first call and reused afterward.
	 *
	 * @return the cached {@code StackTraceFocuser}, never {@code null}
	 */
	public static StackTraceFocuser defaultInstance() {
		StackTraceFocuser instance = defaultInstance;
		if (instance == null) {
			synchronized (StackTraceFocuser.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = builder().build();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/**
	 * Creates a new {@link Builder} instance.
	 *
	 * @return the new builder, never {@code null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	private static List<Predicate<StackTraceElement>> toPredicates(final Collection<Pattern> classNameIgnores) {
		return classNameIgnores.stream().
				map(StackTraceFocuser::ignoreClassName).
				collect(toList());
	}
	
	static Predicate<StackTraceElement> toPredicate(final Collection<Pattern> classNameIgnores) {
		return toPredicate(toPredicates(classNameIgnores));
	}

	/**
	 * Creates a new {@code StackTraceFocuser} ignoring stack frames originating from the JDK.
	 *
	 * @return the new {@code StackTraceFocuser}, never {@code null}
	 */
	public static StackTraceFocuser ignoreJavaClasses() {
		return ignoreClassNames(DEFAULT_JAVA_CLASS_PATTERNS);
	}

	/**
	 * Constructs a new {@code StackTraceFocuser} for the given predicates. All predicates are
	 * combined with logical OR before being negated to decide if a frame remains visible.
	 *
	 * @param ignores the predicates describing frames to ignore, never {@code null}
	 */
	public StackTraceFocuser(final Iterable<Predicate<StackTraceElement>> ignores) {
		ignore = toPredicate(ignores);
	}

	private static Predicate<StackTraceElement> toPredicate(final Iterable<Predicate<StackTraceElement>> ignores) {
		return stream(ignores.spliterator(), false).
				reduce(Predicate::or).
				orElse(frame -> false).
				negate();
	}
	
	/**
	 * Returns a new {@code StackTraceFocuser} that ignores class names from this focuser and the
	 * provided list.
	 *
	 * @param classNameIgnores additional class-name patterns to ignore, never {@code null}
	 * @return a new {@code StackTraceFocuser} containing the combined ignores, never {@code null}
	 */
	public StackTraceFocuser andIgnoreClassNames(final List<Pattern> classNameIgnores) {
		return new StackTraceFocuser(ignore, toPredicate(classNameIgnores));
	}
	
	/**
	 * Constructs a new {@code StackTraceFocuser} for the given predicates.
	 *
	 * @param first the first predicate describing frames to ignore, never {@code null}
	 * @param rest the optional remaining predicates of frames to ignore
	 */
	@SafeVarargs
	public StackTraceFocuser(final Predicate<StackTraceElement> first,
		final Predicate<StackTraceElement>... rest) {
		this(Lists.asList(first, rest));
	}

	/**
	 * Applies the focuser according to the {@link Function} contract, trimming the supplied
	 * throwable in place.
	 *
	 * @param throwable the throwable to focus, may be {@code null}
	 * @return the original throwable with filtered stack traces, or {@code null}
	 */
	@Override
	public Throwable apply(final Throwable throwable) {
		return focus(throwable);
	}

	/**
	 * Applies the focuser, trimming the supplied throwable in place.
	 *
	 * @param throwable the throwable to focus, may be {@code null}
	 * @param <T> the throwable type
	 * @return the original throwable with filtered stack traces, or {@code null}
	 */
	public <T extends Throwable> T applyTyped(final T throwable) {
		return focus(throwable);
	}

	private <T extends Throwable> T focus(final T throwable) {
		if (throwable == null) {
			return null;
		}
		focusInPlace(throwable, Collections.newSetFromMap(new IdentityHashMap<>()));
		return throwable;
	}

	private void focusInPlace(final Throwable throwable, final Set<Throwable> seen) {
		if (throwable == null || seen.contains(throwable)) {
			return;
		}
		seen.add(throwable);
		throwable.setStackTrace(filteredStack(throwable.getStackTrace()));
		focusInPlace(throwable.getCause(), seen);
		for (final Throwable suppressed : throwable.getSuppressed()) {
			focusInPlace(suppressed, seen);
		}
	}

	private StackTraceElement[] filteredStack(final StackTraceElement[] stackTrace) {
		final List<StackTraceElement> found = asList(stackTrace).stream().filter(ignore).collect(toList());
		return found.toArray(new StackTraceElement[found.size()]);
	}

	/**
	 * Creates a predicate that matches stack frames whose class names match the provided pattern.
	 *
	 * @param className the class-name pattern, never {@code null}
	 * @return the predicate, never {@code null}
	 */
	public static Predicate<StackTraceElement> ignoreClassName(final Pattern className) {
		return frame -> className.matcher(frame.getClassName()).find();
	}

	/**
	 * Creates a predicate that matches stack frames whose method names match the provided pattern.
	 *
	 * @param methodName the method-name pattern, never {@code null}
	 * @return the predicate, never {@code null}
	 */
	public static Predicate<StackTraceElement> ignoreMethodName(final Pattern methodName) {
		return frame -> methodName.matcher(frame.getMethodName()).find();
	}

	/**
	 * Creates a predicate that matches stack frames whose file names match the provided pattern.
	 *
	 * @param fileName the file-name pattern, never {@code null}
	 * @return the predicate, never {@code null}
	 */
	public static Predicate<StackTraceElement> ignoreFileName(final Pattern fileName) {
		return frame -> fileName.matcher(frame.getFileName()).find();
	}

	/**
	 * Creates a predicate that matches stack frames whose line numbers match the provided pattern.
	 *
	 * @param lineNumber the line-number pattern, never {@code null}
	 * @return the predicate, never {@code null}
	 */
	public static Predicate<StackTraceElement> ignoreLineNumber(final Pattern lineNumber) {
		return frame -> lineNumber.matcher(String.valueOf(frame.getLineNumber())).find();
	}

	/**
	 * Fluent builder for composing {@link StackTraceFocuser} instances.
	 */
	public static final class Builder {
		private static final List<Pattern> DEFAULT_CLASS_PATTERNS = Lists.newArrayList(
			"^java\\.lang\\.", "^java\\.util\\.", "^javax\\.", "^sun\\.", "^com\\.sun\\.",
			"^io\\.vertx\\.core\\.", "^com\\.mysql\\.cj\\.", "^io\\.netty\\.",
			"^io\\.vertx\\.ext\\.web\\.").stream().map(Pattern::compile).collect(toList());
		private final Set<Pattern> classNamePatterns = new HashSet<>();
		private final Set<Pattern> methodNamePatterns = new HashSet<>();
		private final Set<Pattern> fileNamePatterns = new HashSet<>();
		private boolean skipDefaultPatterns = false;

		private Builder() {
		}

		/**
		 * Adds a class-name pattern to ignore via {@link Pattern}. Example:
		 * {@code StackTraceFocuser.builder().addClass(Pattern.compile("^com\\.example"));}
		 *
		 * @param ignorePattern pattern describing classes to skip
		 * @return this builder
		 */
		public Builder addClass(final Pattern ignorePattern) {
			this.classNamePatterns.add(ignorePattern);
			return this;
		}

		/**
		 * Adds multiple class-name patterns to ignore.
		 * Example: {@code builder.addClasses(List.of(Pattern.compile("^io\\.internal")));}
		 *
		 * @param ignorePatterns patterns describing classes to skip
		 * @return this builder
		 */
		public Builder addClasses(final Collection<Pattern> ignorePatterns) {
			this.classNamePatterns.addAll(ignorePatterns);
			return this;
		}

		/**
		 * Adds a class-name regex that is compiled before use.
		 * Example: {@code builder.addClass("^com\\.example\\.internal");}
		 *
		 * @param ignoreRegex regex describing classes to skip
		 * @return this builder
		 */
		public Builder addClass(final String ignoreRegex) {
			this.classNamePatterns.add(compile(ignoreRegex));
			return this;
		}

		/**
		 * Adds multiple class-name regexes.
		 * Example: {@code builder.addClass("^com\\.example", "^org\\.temp");}
		 *
		 * @param ignoreRegex regexes describing classes to skip
		 * @return this builder
		 */
		public Builder addClass(final String... ignoreRegex) {
			this.classNamePatterns.addAll(Stream.of(ignoreRegex).map(Pattern::compile).collect(toList()));
			return this;
		}

		/**
		 * Adds a method-name pattern to ignore. Example:
		 * {@code builder.addMethod(Pattern.compile("^lambda$"));}
		 * To match a literal {@code $} (common in lambda method names), escape it in the regex:
		 * {@code builder.addMethod(Pattern.compile("^lambda\\$get\\$2$"));}
		 *
		 * @param ignorePattern pattern describing methods to skip
		 * @return this builder
		 */
		public Builder addMethod(final Pattern ignorePattern) {
			this.methodNamePatterns.add(ignorePattern);
			return this;
		}

		/**
		 * Adds a method-name regex to ignore.
		 * Example: {@code builder.addMethod("^dispatch$");}
		 * To match a literal {@code $}, escape it in the regex:
		 * {@code builder.addMethod("^lambda\\$get\\$2$");}
		 *
		 * @param ignoreRegex regex describing methods to skip
		 * @return this builder
		 */
		public Builder addMethod(final String ignoreRegex) {
			this.methodNamePatterns.add(compile(ignoreRegex));
			return this;
		}

		/**
		 * Adds multiple method-name regexes in one call.
		 * Example: {@code builder.addMethod("^lambda$", "^helper$");}
		 * To match a literal {@code $}, escape it in the regex:
		 * {@code builder.addMethod("^lambda\\$get\\$2$");}
		 *
		 * @param ignoreRegex regexes describing methods to skip
		 * @return this builder
		 */
		public Builder addMethod(final String... ignoreRegex) {
			this.methodNamePatterns.addAll(Stream.of(ignoreRegex).map(Pattern::compile).collect(toList()));
			return this;
		}

		/**
		 * Adds a collection of method-name patterns to ignore.
		 * Example: {@code builder.addMethods(customMethodPatterns);}
		 *
		 * @param ignorePatterns patterns describing methods to skip
		 * @return this builder
		 */
		public Builder addMethods(final Collection<Pattern> ignorePatterns) {
			this.methodNamePatterns.addAll(ignorePatterns);
			return this;
		}

		/**
		 * Adds a file-name pattern to ignore.
		 * Example: {@code builder.addFile(Pattern.compile("^Generated.*"));}
		 *
		 * @param ignorePattern pattern describing files to skip
		 * @return this builder
		 */
		public Builder addFile(final Pattern ignorePattern) {
			this.fileNamePatterns.add(ignorePattern);
			return this;
		}

		/**
		 * Adds a file-name regex to ignore.
		 * Example: {@code builder.addFile(".*Proxy\\.java");}
		 *
		 * @param ignoreRegex regex describing files to skip
		 * @return this builder
		 */
		public Builder addFile(final String ignoreRegex) {
			this.fileNamePatterns.add(compile(ignoreRegex));
			return this;
		}

		/**
		 * Adds multiple file-name regexes in one step.
		 * Example: {@code builder.addFile(".*Proxy\\.java", "^Synthetic.*\\.java");}
		 *
		 * @param ignoreRegex regexes describing files to skip
		 * @return this builder
		 */
		public Builder addFile(final String... ignoreRegex) {
			this.fileNamePatterns.addAll(Stream.of(ignoreRegex).map(Pattern::compile).collect(toList()));
			return this;
		}

		/**
		 * Adds a collection of file-name patterns to ignore.
		 * Example: {@code builder.addFiles(testFilePatterns);}
		 *
		 * @param ignorePatterns patterns describing files to skip
		 * @return this builder
		 */
		public Builder addFiles(final Collection<Pattern> ignorePatterns) {
			this.fileNamePatterns.addAll(ignorePatterns);
			return this;
		}

		/**
		 * Skips the built-in default ignore patterns so only user-specified predicates apply.
		 * Example: {@code builder.skipDefaultPatterns().addClass("^com\\.example");}
		 *
		 * @return this builder
		 */
		public Builder skipDefaultPatterns() {
			this.skipDefaultPatterns = true;
			return this;
		}

		/**
		 * Builds the {@link StackTraceFocuser} using the configured predicates.
		 * Example: {@code StackTraceFocuser focuser = builder.addClass("^com\\.app").build();}
		 *
		 * @return configured focuser
		 */
		public StackTraceFocuser build() {
			final Set<Pattern> finalClassPatterns = new HashSet<>(classNamePatterns);
			if (!skipDefaultPatterns) {
				finalClassPatterns.addAll(DEFAULT_CLASS_PATTERNS);
			}

			final List<Predicate<StackTraceElement>> predicates = Lists.newArrayList();
			predicates.addAll(finalClassPatterns.stream().map(StackTraceFocuser::ignoreClassName).collect(toList()));
			predicates.addAll(methodNamePatterns.stream().map(StackTraceFocuser::ignoreMethodName)
				.collect(toList()));
			predicates.addAll(fileNamePatterns.stream().map(StackTraceFocuser::ignoreFileName)
				.collect(toList()));

			return new StackTraceFocuser(predicates);
		}
	}
}
