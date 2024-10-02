package com.inqwise.errors;

import java.util.List;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.inqwise.errors.spi.ErrorCodeProvider;

class ErrorCodeProviders {
	private static final Logger logger = LogManager.getLogger(ErrorCodeProviders.class);
	
	private static ServiceLoader<ErrorCodeProvider> providers;
	
	private synchronized static void load() {
		if (providers == null || !providers.iterator().hasNext()) {
			logger.debug("load");
			providers = ServiceLoader.load(ErrorCodeProvider.class);
			logger.debug("found {} provider(s)", providers.stream().count());
		}
	}
	
	public synchronized static ErrorCodeProvider get(String group) {
		load();
		for (var next : providers) {
			if (next.group().equalsIgnoreCase(group)) {
				return next;
			}
		}
		
		logger.warn("Not found ErrorCodeProvider with group: '{}'", group);
		return null;
	}
	
	public synchronized static List<ErrorCodeProvider> getAll() {
		load();
		return Lists.newArrayList(providers.iterator());
	}
}
