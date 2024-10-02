package com.inqwise.errors;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

public final class Throws {
	public static RuntimeException propagate(Throwable ex) throws RuntimeException,Error {
		Throwables.throwIfUnchecked(ex);
		throw new RuntimeException(ex);
	}
	
	public static NotImplementedException notImplemented() {
		return notImplemented("not implemented");
	}
	
	public static NotImplementedException notImplemented(String message) {
		return new NotImplementedException(message);
	}

	public static Throwable unbox(Throwable ex, Class<? extends Throwable> exceptionClass) {
		Throwable resEx = ex;
		while(exceptionClass.isInstance(resEx)) {
			resEx = resEx.getCause();
		}
		return resEx;
	}
	
	public static Throwable unbox(Throwable ex, Class<? extends Throwable> exceptionClass1, Class<? extends Throwable> exceptionClass2) {
		Throwable resEx = ex;
		while(exceptionClass1.isInstance(resEx) || exceptionClass2.isInstance(resEx)) {
			resEx = resEx.getCause();
		}
		return resEx;
	}
	
	public static Throwable unbox(Throwable ex, Class<? extends Throwable>... elements) {
		AtomicReference<Throwable> resEx = new AtomicReference<>(ex);
		Set<Class<? extends Throwable>> classes = Sets.newHashSet(elements);
		while(null != resEx.get().getCause() && classes.stream().anyMatch(t -> t.isInstance(resEx.get()))) {
			resEx.set(resEx.get().getCause());
		}
		return resEx.get();
	}
}
