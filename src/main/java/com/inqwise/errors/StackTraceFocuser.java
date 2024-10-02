package com.inqwise.errors;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

public final class StackTraceFocuser<E extends Throwable>
	   implements Function<E, E> {
    private static final List<Pattern> defaultClassNameIgnores = asList(compile("^java\\."),
		  compile("^javax\\."), compile("^sun\\."), compile("^com\\.sun\\."));
    private final Predicate<StackTraceElement> ignore;

    /**
	* Creates a new {@code StackTraceFocuser} for the given list of <var>classNameIgnores</var>
	* regexen.
	*
	* @param classNameIgnores the patterns to ignore, never missing
	* @param <E> the exception type
	*
	* @return thew new {@code StackTraceFocuser}, never missing
	*/
    
    public static <E extends Throwable> StackTraceFocuser<E> ignoreClassNames(
		   final List<Pattern> classNameIgnores) {
	   return new StackTraceFocuser<>(toPredicates(classNameIgnores));
    }

	private static List<Predicate<StackTraceElement>> toPredicates(final List<Pattern> classNameIgnores) {
		return classNameIgnores.stream().
				 map(StackTraceFocuser::ignoreClassName).
				 collect(toList());
	}
	
	private static Predicate<StackTraceElement> toPredicate(final List<Pattern> classNameIgnores) {
		return toPredicate(toPredicates(classNameIgnores));
	}

    /**
	* Creates a new, default {@code StackTraceFocuser} ignoring frames from the JDK.
	*
	* @param <E> the exception type
	*
	* @return thew new {@code StackTraceFocuser}, never missing
	*/
    
    public static <E extends Throwable> StackTraceFocuser<E> ignoreJavaClasses() {
	   return ignoreClassNames(defaultClassNameIgnores);
    }

    /**
	* Constructs a new {@code StackTraceFocuser} for the given <var>ignores</var> predicates.  All
	* predicates are or'ed when checking if a frame should be ignored.
	*
	* @param ignores the predicates of frames to ignore, never missing
	*/
    public StackTraceFocuser( final Iterable<Predicate<StackTraceElement>> ignores) {
	   ignore = toPredicate(ignores);
    }

	private static Predicate<StackTraceElement> toPredicate(final Iterable<Predicate<StackTraceElement>> ignores) {
		return stream(ignores.spliterator(), false).
				 reduce(Predicate::or).
				 orElse(frame -> false).
				 negate();
	}
    
    public <E extends Throwable> StackTraceFocuser<E> andIgnoreClassNames(final List<Pattern> classNameIgnores) {
    	return new StackTraceFocuser<>(ignore, toPredicate(classNameIgnores));
    }
    
    /**
	* Constructs a new {@code StackTraceFocuser} for the given <var>ignores</var> predicates.
	*
	* @param first the first predicate of frames to ignore, never missing
	* @param rest the optional remaining predicates of frames to ignore
	*/
    @SafeVarargs
    public StackTraceFocuser( final Predicate<StackTraceElement> first,
		  final Predicate<StackTraceElement>... rest) {
	   this(Lists.asList(first, rest));
    }

    @Override
    public E apply(final E e) {
	   stream(LinkedIterable.over(e, Objects::isNull, Throwable::getCause).spliterator(), true).
			 forEach(x -> {
				final List<StackTraceElement> found = asList(x.getStackTrace()).stream().
					   filter(ignore).
					   collect(toList());
				try {
					x.setStackTrace(found.toArray(new StackTraceElement[found.size()]));
				} catch (Throwable t) {}
			 });
	   return e;
    }

    
    public static Predicate<StackTraceElement> ignoreClassName( final Pattern className) {
	   return frame -> className.matcher(frame.getClassName()).find();
    }

    
    public static Predicate<StackTraceElement> ignoreMethodName( final Pattern methodName) {
	   return frame -> methodName.matcher(frame.getMethodName()).find();
    }

    
    public static Predicate<StackTraceElement> ignoreFileName( final Pattern fileName) {
	   return frame -> fileName.matcher(frame.getFileName()).find();
    }

    
    public static Predicate<StackTraceElement> ignoreLineNumber( final Pattern lineNumber) {
	   return frame -> lineNumber.matcher(String.valueOf(frame.getLineNumber())).find();
    }
}