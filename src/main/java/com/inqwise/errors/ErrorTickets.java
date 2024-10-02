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

public final class ErrorTickets {
	private ErrorTickets(){};

	public static ErrorTicket notFound(Object details) {
		String str;
		if(details instanceof Throwable) {
			str = Throwables.getRootCause((Throwable)details).getMessage();
		} else {
			str = details.toString();
		}
		return ErrorTicket.builder().withError(ErrorCodes.NotFound).withErrorDetails(str).build();
	}

	public static ErrorTicket general(String details) {
		return ErrorTicket.builder().withError(ErrorCodes.GeneralError).withErrorDetails(details).build();
	}

	public static void checkAnyNotNull(Object[] values, Object errorMessage) {
		checkAnyNotNull(Lists.newArrayList(values), errorMessage);
	}

	public static void checkAnyNotNull(Iterable<Object> values, Object errorMessage) {

		if (values == null || Iterables.all(values, Objects::isNull)) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
	}

	public static void checkAnyNotNull(Object[] values, Consumer<ErrorTicket.Builder> consumer) {
		checkAnyNotNull(List.of(values), consumer);
	}

	public static void checkAnyNotNull(Iterable<Object> values, Consumer<ErrorTicket.Builder> consumer) {
		if (values == null || Iterables.all(values, Objects::isNull)) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
	}

	public static void checkAllNotNull(Iterable<?> values, Object errorMessage) {
		if (values == null || Iterables.any(values, Objects::isNull)) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
	}
	
	public static void checkAllNotNull(Object[] values, Consumer<ErrorTicket.Builder> consumer) {
		checkAllNotNull(List.of(values), consumer);
	}
	
	public static void checkAllNotNull(Iterable<?> values, Consumer<ErrorTicket.Builder> consumer) {
		if (values == null || Iterables.any(values, Objects::isNull)) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
	}
	
	public static <T> T checkNotNull(T reference, Object errorMessage) {
		if (reference == null) {
			throw ErrorTicket.builder().withError(ErrorCodes.ArgumentNull).withErrorDetails(String.valueOf(errorMessage)).build();
		}
		return reference;
	}
	
	public static <T> T checkNotNull(T reference, Object errorMessage, ErrorCode errorCode) {
		Preconditions.checkNotNull(errorCode, ErrorTicket.Keys.CODE);
		if (reference == null) {
			throw ErrorTicket.builder().withError(errorCode).withErrorGroup(errorCode.group()).withErrorDetails(String.valueOf(errorMessage)).build();
		}
		return reference;
	}

	public static <T> T checkNotNull(T reference, Consumer<ErrorTicket.Builder> consumer) {
		if (reference == null) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentNull);
			consumer.accept(builder);
			throw builder.build();
		}
		return reference;
	}

	public static <T> Function<T, T> thenCheckNotNull(Object errorMessage) {
		return reference -> checkNotNull(reference, errorMessage);
	}
	
	public static <T> Function<T, T> thenCheckNotNull(Consumer<ErrorTicket.Builder> consumer) {
		return reference -> checkNotNull(reference, consumer);
	}
	
	public static void checkArgument(boolean expression, Object details) {
		checkArgument(expression, b -> b.withErrorDetails(details.toString()));
	}
	
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

	public static void checkArgument(boolean expression, Consumer<ErrorTicket.Builder> builderConsumer) {
		if (! expression) {
			var builder = ErrorTicket.builder().withError(ErrorCodes.ArgumentWrong);
			builderConsumer.accept(builder);
			throw builder.build();
		}
	}

	public static ErrorTicket noLoggedIn() {
		return ErrorTicket.builder().withError(ErrorCodes.NotLoggedIn).withStatusCode(401).build();
	}

	public static ErrorTicket notImplemented(String pattern, Object... arguments) {
		return notImplemented(ParameterizedMessage.format(pattern, arguments));
	}

	public static ErrorTicket notImplemented(String message) {
		return ErrorTicket.builder().withError(ErrorCodes.NotImplemented).withStatusCode(501).withErrorDetails(message).build();
	}
}
