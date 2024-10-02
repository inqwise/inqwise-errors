package com.inqwise.errors;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

public class ExceptionNormalizer {

	private final StackTraceFocuser<Throwable> stackTraceFocuser;

	static final List<Pattern> BASE_IGNORE_CLASSES_PATERN_LIST = Lists.newArrayList("^java\\.lang\\.","^java\\.util\\.","^javax\\.","^sun\\.","^com\\.sun\\.","^io\\.vertx\\.core\\.", "^com\\.mysql\\.cj\\.","^io\\.netty\\.","^io\\.vertx\\.ext\\.web\\.","^com\\.tamal\\.payroll\\.ext\\.error\\.").stream().map(Pattern::compile).collect(Collectors.toList());


	public static ExceptionNormalizer notmalizer() {
		return new ExceptionNormalizer();
	}

	public ExceptionNormalizer() {
		stackTraceFocuser = StackTraceFocuser.ignoreClassNames(BASE_IGNORE_CLASSES_PATERN_LIST);
	}

	public Throwable normalize(Throwable t) {
		Throwable result = stackTraceFocuser.apply(Throws.unbox(t, CompletionException.class));
		
		return result;
	}

	public StackTraceFocuser<Throwable> stackTraceFocuser() {
		return stackTraceFocuser;
	}
}
