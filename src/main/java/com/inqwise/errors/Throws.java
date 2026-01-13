package com.inqwise.errors;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/** Utility helpers for common exception workflow patterns. */
public final class Throws {
	private Throws() {}

	/**
	 * Rethrows checked exceptions as runtime ones.
	 *
	 * @param ex throwable to propagate
	 * @return runtime exception wrapping {@code ex}
	 */
	public static RuntimeException propagate(Throwable ex) throws RuntimeException,Error {
		Throwables.throwIfUnchecked(ex);
		throw new RuntimeException(ex);
	}
	
	/**
	 * Creates a {@link NotImplementedException} with the default message.
	 *
	 * @return the created exception
	 */
	public static NotImplementedException notImplemented() {
		return notImplemented("not implemented");
	}
	
	/**
	 * Creates a {@link NotImplementedException} describing the missing operation.
	 *
	 * @param message detail message
	 * @return the created exception
	 */
	public static NotImplementedException notImplemented(String message) {
		return new NotImplementedException(message);
	}

	/**
	 * Convenience factory for {@link NotFoundException}.
	 *
	 * @param message format string
	 * @param args parameters for the format
	 * @return the created exception
	 */
	public static NotFoundException notFound(String message, Object... args) {
		return new NotFoundException(message, args);
	}
	
	/**
	 * Removes wrapper exceptions of the provided type from the chain.
	 *
	 * @param ex throwable to unwrap
	 * @param exceptionClass wrapper type to peel
	 * @return the first non-matching throwable
	 */
	public static Throwable unbox(Throwable ex, Class<? extends Throwable> exceptionClass) {
		Throwable resEx = ex;
		while(exceptionClass.isInstance(resEx)) {
			resEx = resEx.getCause();
		}
		return resEx;
	}
	
	/**
	 * Unwraps when either type matches.
	 *
	 * @param ex throwable to unwrap
	 * @param exceptionClass1 first wrapper type
	 * @param exceptionClass2 second wrapper type
	 * @return the first non-matching throwable
	 */
	public static Throwable unbox(Throwable ex, Class<? extends Throwable> exceptionClass1, Class<? extends Throwable> exceptionClass2) {
		Throwable resEx = ex;
		while(exceptionClass1.isInstance(resEx) || exceptionClass2.isInstance(resEx)) {
			resEx = resEx.getCause();
		}
		return resEx;
	}
	
	/**
	 * Unwraps repeatedly while the exception matches any provided type.
	 *
	 * @param ex throwable to unwrap
	 * @param elements wrapper types
	 * @return the first non-matching throwable
	 */
	public static Throwable unbox(Throwable ex, Class<? extends Throwable>... elements) {
		AtomicReference<Throwable> resEx = new AtomicReference<>(ex);
		Set<Class<? extends Throwable>> classes = Sets.newHashSet(elements);
		while(null != resEx.get().getCause() && classes.stream().anyMatch(t -> t.isInstance(resEx.get()))) {
			resEx.set(resEx.get().getCause());
		}
		return resEx.get();
	}
}
