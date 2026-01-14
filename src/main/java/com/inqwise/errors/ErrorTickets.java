package com.inqwise.errors;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.message.ParameterizedMessage;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Convenience guard/validation helpers that throw {@link ErrorTicket}s.
 */
public final class ErrorTickets {
	private ErrorTickets(){};

	/**
	 * @deprecated prefer throwing {@link NotFoundException} or creating a custom ticket.
	 * @param details additional context for the error
	 * @return constructed ticket
	 */
	@Deprecated
	public static ErrorTicket notFound(Object details) {
		String str;
		if(details instanceof Throwable) {
			str = Throwables.getRootCause((Throwable)details).getMessage();
		} else {
			str = details.toString();
		}
		return ErrorTicket.builder().withError(ErrorCodes.NotFound).withErrorDetails(str).build();
	}

	/**
	 * Creates a general error ticket with the provided details.
	 *
	 * @param details description of the failure
	 * @return constructed ticket
	 */
	public static ErrorTicket general(String details) {
		return ErrorTicket.builder().withError(ErrorCodes.GeneralError).withErrorDetails(details).build();
	}

	/**
	 * Throws {@link ErrorTicket} when every provided value is {@code null}.
	 *
	 * @param values values to inspect
	 * @param errorMessage message used when throwing
	 */
	public static void checkAnyNotNull(Object[] values, Object errorMessage) {
		checkAnyNotNull(Lists.newArrayList(values), errorMessage);
	}

	/**
	 * Throws when every element in the iterable is {@code null}.
	 *
	 * @param values iterable to inspect
	 * @param errorMessage message used when throwing
	 */
	public static void checkAnyNotNull(Iterable<Object> values, Object errorMessage) {

		if (values == null || Iterables.all(values, Objects::isNull)) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
	}

	/**
	 * Variant that allows customizing the thrown ticket when values are all {@code null}.
	 *
	 * @param values values to inspect
	 * @param consumer builder customizer
	 */
	public static void checkAnyNotNull(Object[] values, Consumer<ErrorTicket.Builder> consumer) {
		checkAnyNotNull(List.of(values), consumer);
	}

	/**
	 * Iterable overload for {@link #checkAnyNotNull(Object[], Consumer)}.
	 *
	 * @param values values to inspect
	 * @param consumer builder customizer
	 */
	public static void checkAnyNotNull(Iterable<Object> values, Consumer<ErrorTicket.Builder> consumer) {
		if (values == null || Iterables.all(values, Objects::isNull)) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
	}

	/**
	 * Ensures no element in the iterable is {@code null}.
	 *
	 * @param values values to inspect
	 * @param errorMessage message used when throwing
	 */
	public static void checkAllNotNull(Iterable<?> values, Object errorMessage) {
		if (values == null || Iterables.any(values, Objects::isNull)) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
	}
	
	/**
	 * Array overload for {@link #checkAllNotNull(Iterable, Consumer)}.
	 *
	 * @param values values to inspect
	 * @param consumer builder customizer
	 */
	public static void checkAllNotNull(Object[] values, Consumer<ErrorTicket.Builder> consumer) {
		checkAllNotNull(List.of(values), consumer);
	}
	
	/**
	 * Configurable overload ensuring every element is non-null.
	 *
	 * @param values values to inspect
	 * @param consumer builder customizer
	 */
	public static void checkAllNotNull(Iterable<?> values, Consumer<ErrorTicket.Builder> consumer) {
		if (values == null || Iterables.any(values, Objects::isNull)) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
	}
	
	/**
	 * Guard returning the reference when non-null or throws {@link ErrorTicket}.
	 *
	 * @param reference value to inspect
	 * @param errorMessage message used when throwing
	 * @param <T> value type
	 * @return the original reference when non-null
	 */
	public static <T> T checkNotNull(T reference, Object errorMessage) {
		if (reference == null) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
		return reference;
	}
	
	/**
	 * Same as {@link #checkNotNull(Object, Object)} but with a custom {@link ErrorCode}.
	 *
	 * @param reference value to inspect
	 * @param errorMessage message used when throwing
	 * @param errorCode custom error code
	 * @param <T> value type
	 * @return reference when non-null
	 */
	public static <T> T checkNotNull(T reference, Object errorMessage, ErrorCode errorCode) {
		Preconditions.checkNotNull(errorCode, ErrorTicket.Keys.CODE);
		if (reference == null) {
			throw ErrorTicket.builder().withError(errorCode).withErrorGroup(errorCode.group()).withErrorDetails(String.valueOf(errorMessage)).build();
		}
		return reference;
	}

	/**
	 * Customizable version of {@link #checkNotNull(Object, Object)}.
	 *
	 * @param reference value to inspect
	 * @param consumer customizer for the thrown ticket
	 * @param <T> value type
	 * @return reference when non-null
	 */
	public static <T> T checkNotNull(T reference, Consumer<ErrorTicket.Builder> consumer) {
		if (reference == null) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
		return reference;
	}

	/**
	 * Returns a function performing {@link #checkNotNull(Object, Object)}.
	 *
	 * @param errorMessage message used when throwing
	 * @param <T> value type
	 * @return unary function enforcing non-null values
	 */
	public static <T> Function<T, T> thenCheckNotNull(Object errorMessage) {
		return reference -> checkNotNull(reference, errorMessage);
	}
	
	/**
	 * Returns a function performing {@link #checkNotNull(Object, Consumer)}.
	 *
	 * @param consumer customizer for the thrown ticket
	 * @param <T> value type
	 * @return enforcing function
	 */
	public static <T> Function<T, T> thenCheckNotNull(Consumer<ErrorTicket.Builder> consumer) {
		return reference -> checkNotNull(reference, consumer);
	}
	
	/**
	 * Throws {@link ErrorTicket} when expression is false with the provided details.
	 *
	 * @param expression condition to validate
	 * @param details failure description
	 */
	public static void checkArgument(boolean expression, Object details) {
		checkArgument(expression, b -> b.withErrorDetails(details.toString()));
	}
	
	/**
	 * Custom error-code version of {@link #checkArgument(boolean, Object)}.
	 *
	 * @param expression condition to validate
	 * @param details failure description
	 * @param errorCode custom error code
	 */
	public static void checkArgument(boolean expression, Object details, ErrorCode errorCode) {
		Preconditions.checkNotNull(errorCode, ErrorTicket.Keys.CODE);
		if (! expression) {
			throw ErrorTicket.builder()
				.withError(errorCode)
				.withErrorGroup(errorCode.group())
				.withErrorDetails(details.toString())
				.build();
		}
	}

	/**
	 * Allows caller to mutate the {@link ErrorTicket} when the expression is false.
	 *
	 * @param expression condition to validate
	 * @param builderConsumer mutator for the thrown ticket
	 */
	public static void checkArgument(boolean expression, Consumer<ErrorTicket.Builder> builderConsumer) {
		if (! expression) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentWrong);
			builderConsumer.accept(builder);
			throw builder.build();
		}
	}

	/**
	 * @deprecated prefer domain-specific authentication handling.
	 * @return constructed ticket
	 */
	@Deprecated
	public static ErrorTicket noLoggedIn() {
		return ErrorTicket.builder().withError(ErrorCodes.NotLoggedIn).withStatusCode(401).build();
	}

	/**
	 * @deprecated prefer {@link NotImplementedException}.
	 * @param pattern format string
	 * @param arguments format arguments
	 * @return constructed ticket
	 */
	@Deprecated
	public static ErrorTicket notImplemented(String pattern, Object... arguments) {
		return notImplemented(ParameterizedMessage.format(pattern, arguments));
	}

	/**
	 * @deprecated prefer {@link NotImplementedException}.
	 * @param message detail message
	 * @return constructed ticket
	 */
	@Deprecated
	public static ErrorTicket notImplemented(String message) {
		return ErrorTicket.builder().withError(ErrorCodes.NotImplemented).withStatusCode(501).withErrorDetails(message).build();
	}
}
