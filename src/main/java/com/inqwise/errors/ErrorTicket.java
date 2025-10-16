package com.inqwise.errors;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.message.ParameterizedMessage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Enums;
import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;

@JsonAutoDetect(getterVisibility=Visibility.NONE, isGetterVisibility = Visibility.NONE)
public class ErrorTicket extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3636672196437415424L;
	
	private final static int UNIQUE_IDENTIFIER = 2057221941;
	
	public static int getUniqueIdentifier() {
		return UNIQUE_IDENTIFIER;
	}

public static class Keys {
		public static final String CODE = "code";
		public static final String DETAILS = "details";
		public static final String ERROR_GROUP = "group";
		public static final String ERROR_ID = "id";
		public static final String STATUS_CODE = "status_code";
		
		// RFC 7807 standard fields
		public static final String TYPE = "type";
		public static final String TITLE = "title";
		public static final String STATUS = "status";
		public static final String DETAIL = "detail";
		public static final String INSTANCE = "instance";
		
		// OAuth 2.0 fields (RFC 6749)
		public static final String ERROR = "error";
		public static final String ERROR_DESCRIPTION = "error_description";
		public static final String ERROR_URI = "error_uri";
	}

	void setErrorId(UUID errorId) {
		this.errorId = errorId;
	}
	
	void setError(ErrorCode error) {
		this.error = error;
	}

	void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	void setErrorGroup(String group) {
		this.errorGroup = group;
	}
	
	@JsonProperty(Keys.ERROR_ID)
	protected UUID errorId;
	@JsonProperty(Keys.DETAILS)
	protected String errorDetails;
	@JsonProperty(Keys.CODE)
	protected ErrorCode error;
	@JsonProperty(Keys.ERROR_GROUP)
	protected String errorGroup;
    @JsonProperty(Keys.STATUS_CODE)
	private Integer statusCode;
	
	// RFC 7807 fields
	@JsonProperty(Keys.TYPE)
	protected String type;
	@JsonProperty(Keys.TITLE)
	protected String title;
	@JsonProperty(Keys.INSTANCE)
	protected String instance;
	
	// Additional metadata
	protected Map<String, Object> extensions;

	private ErrorTicket(Builder builder) {
		this.errorId = builder.errorId;
		this.errorDetails = builder.errorDetails;
		this.error = builder.error;
		this.errorGroup = builder.errorGroup;
		this.statusCode = builder.statusCode;
		this.type = builder.type;
		this.title = builder.title;
		this.instance = builder.instance;
		this.extensions = builder.extensions;
	}

	@JsonCreator
    public static ErrorTicket fromMap(Map<String,Object> map) {
		return new ErrorTicket(new JsonObject(map));
	}
	
	public static final ErrorTicket parse(JsonObject json) {
		System.out.println(json);
		return parse(json, null);
	}
	
	public static final ErrorTicket parse(JsonObject json, String defaultGroup) {
		var builder = ErrorTicket.builder();
		builder.withErrorDetails(json.getString(Keys.DETAILS));
		builder.withErrorId(Optional.ofNullable(json.getString(Keys.ERROR_ID)).map(UUID::fromString).orElse(null));
		builder.withStatusCode(json.getInteger(Keys.STATUS_CODE));
		String errorGroup;
		builder.withErrorGroup(errorGroup = json.getString(Keys.ERROR_GROUP, defaultGroup));
		
		var errorCodeName = json.getString(Keys.CODE);
		ErrorCode error = null;
		if(null != errorCodeName) {
			if (null == errorGroup) {
				error = Enums.getIfPresent(ErrorCodes.class, errorCodeName).orNull();
				if(null == error) {
					throw new Bug("group is mandatory when code provided. '{}'", errorCodeName);
				}
			}
			else {
				var provider = ErrorCodeProviders.get(errorGroup);
				Objects.requireNonNull(provider, "provider not found for group '" + errorGroup + "'");
				error = provider.valueOf(errorCodeName);
				
				if(null == error) {
					throw new Bug("error not found in group. error: '{}', group: '{}'", errorCodeName, errorGroup);
				}
			}
			builder.withError(error);
		}
		
		return builder.build();
	}
	
	public ErrorTicket(JsonObject json) {
		this.errorDetails = json.getString(Keys.DETAILS);
		this.errorId = Optional.ofNullable(json.getString(Keys.ERROR_ID)).map(UUID::fromString).orElse(null);
		this.statusCode = json.getInteger(Keys.STATUS_CODE);
		this.errorGroup = json.getString(Keys.ERROR_GROUP);
		var errorCodeName = json.getString(Keys.CODE);
		if(null != errorCodeName) {
			if (null == errorGroup) {
				this.error = Enums.getIfPresent(ErrorCodes.class, errorCodeName).orNull();
			}
			else {
				var provider = ErrorCodeProviders.get(errorGroup);
				if(null != provider) {
					this.error = provider.valueOf(errorCodeName);
				}
			}
			
			if(null == this.error) {
				this.error = new UndefinedErrorCode(errorCodeName, this.errorGroup);
			}
		}
	}
	
	public <T extends ErrorCode> T getErrorUnsafe() {
		return (T)error;
	}
	
	public ErrorCode getError() {
		return error;
	}
	
	public <T extends ErrorCode> T getError(Class<T> type) {
		Objects.requireNonNull(type);
		return type.cast(error);
	}
	
	public <T extends ErrorCode> T optError(Class<T> type) {
		Objects.requireNonNull(type);
		if (error != null && type.isInstance(error)) {
	        return getError(type);
	    }
	    return null;
	}
	
	public ErrorCodes getErrorAsErrorCodes() {
		if(null != error && error instanceof ErrorCodes) {
			return (ErrorCodes)error;
		} else {
			return ErrorCodes.GeneralError;
		}
	}
	
	public UUID getErrorId() {
		return errorId;
	}

	public String getErrorDetails(){
		return errorDetails;
	}
	
	public String getErrorGroup() {
		return errorGroup;
	}

	public boolean hasErrorExcept(ErrorCode... notErrors){
		return null != notErrors && !Arrays.stream(notErrors).anyMatch((itm) -> itm == error);
	}

	public boolean hasError(ErrorCode... errors){
		return null != errors && Arrays.stream(errors).anyMatch((itm) -> itm == error);
	}

	public JsonObject toJson(){
		var json = new JsonObject();
		
		// Standard fields
		if(null != error) {
			json.put(Keys.CODE, error.toString());
			
			// Map to OAuth error format if it's an OAuth error
			if ("oauth".equals(errorGroup)) {
				json.put(Keys.ERROR, error.toString());
				if (null != errorDetails) {
					json.put(Keys.ERROR_DESCRIPTION, errorDetails);
				}
				if (null != type) {
					json.put(Keys.ERROR_URI, type);
				}
			}
		}
		
		// RFC 7807 fields
		if(null != type) {
			json.put(Keys.TYPE, type);
		}
		
		if(null != title) {
			json.put(Keys.TITLE, title);
		}
		
		if(null != errorDetails) {
			json.put(Keys.DETAILS, errorDetails);
			json.put(Keys.DETAIL, errorDetails); // RFC 7807 field
		}
		
		if(null != errorGroup) {
			json.put(Keys.ERROR_GROUP, errorGroup);
		}

		if(null != errorId) {
			json.put(Keys.ERROR_ID, errorId);
		}

		if(null != statusCode) {
			json.put(Keys.STATUS_CODE, statusCode);
			json.put(Keys.STATUS, statusCode); // RFC 7807 field
		}
		
		if(null != instance) {
			json.put(Keys.INSTANCE, instance);
		}
		
		// Add any extensions
		if (null != extensions) {
			extensions.forEach(json::put);
		}

		return json;
	}
	
public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * Gets the media type for this error response.
	 * 
	 * For RFC 7807 Problem Details, returns application/problem+json
	 * For OAuth 2.0 errors, returns application/json
	 */
public String getContentType() {
		return type != null || "oauth".equals(errorGroup) ? "application/json" : "application/problem+json";
	}
	
	/**
	 * Returns a map of HTTP response headers that should be included with this error.
	 */
	public Map<String, String> getResponseHeaders() {
		Map<String, String> headers = new java.util.HashMap<>();
		
		// Add WWW-Authenticate header for OAuth errors
		if ("oauth".equals(errorGroup) && statusCode != null && statusCode == 401) {
			StringBuilder wwwAuth = new StringBuilder("Bearer");
			if (error != null) {
				wwwAuth.append(" error=\"").append(error.toString()).append("\"");
			}
			if (errorDetails != null) {
				wwwAuth.append(" error_description=\"").append(errorDetails).append("\"");
			}
			if (type != null) {
				wwwAuth.append(" error_uri=\"").append(type).append("\"");
			}
			headers.put("WWW-Authenticate", wwwAuth.toString());
		}
		
		return headers;
	}
	
	/**
	 * Returns true if this error handler supports localization.
	 */
	public boolean supportsLocalization() {
		return true; // Base implementation supports i18n
	}

public Optional<Integer> optStatusCode(){
		return Optional.ofNullable(statusCode);
	}

	/**
	 * Gets the HTTP status code for this error.
	 */
	public Integer getStatus() {
		return statusCode;
	}

	@Override
	public String getMessage() {
		return String.format("%s:%s", getErrorUnsafe(), getErrorDetails());
	}

	public static ErrorTicket propagate(Throwable t, Consumer<ErrorTicket.Builder> creator){
		if(t instanceof ErrorTicket){
			return (ErrorTicket)t;
		}

		if(t instanceof NotFoundException) {
			return ((NotFoundException) t).toErrorTicket();
		}

		ErrorTicket.Builder builder = builder();
		if(null == creator) {
			builder.withError(ErrorCodes.GeneralError);

			if(null == t.getMessage()) {
				if(t instanceof NullPointerException) {
					builder.withDetails("NullPointer");
				} else {
					builder.withDetails(t.getClass().getName());
				}
			} else {
				builder.withDetails("{}:{}", t.getClass().getName(), t.getMessage());
			}
		} else {
			creator.accept(builder);
		}
		return builder.build();
	}

	public static ErrorTicket propagate(Throwable t) {
		return propagate(t, null);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builderFrom(ErrorTicket errorTicket) {
		return new Builder(errorTicket);
	}

public static final class Builder {
		private UUID errorId = null;
		private String errorDetails;
		private ErrorCode error;
		private String errorGroup;
		private Integer statusCode;
		
		// RFC 7807 fields
		private String type;
		private String title;
		private String instance;
		
		// Extensions for RFC 7807 and OAuth 2.0
		private Map<String, Object> extensions = new java.util.HashMap<>();

		private Builder() {
		}

		private Builder(ErrorTicket errorTicket) {
			this.errorId = errorTicket.errorId;
			this.errorDetails = errorTicket.errorDetails;
			this.error = errorTicket.error;
			this.errorGroup = errorTicket.errorGroup;
			this.statusCode = errorTicket.statusCode;
		}

		public Builder withErrorId(UUID errorId) {
			this.errorId = errorId;
			return this;
		}

		public Builder withErrorDetails(String errorDetails) {
			this.errorDetails = errorDetails;
			return this;
		}

		public Builder withError(ErrorCode error) {
			this.error = error;
			return this;
		}

		public Builder withErrorGroup(String errorGroup) {
			this.errorGroup = errorGroup;
			return this;
		}

	public Builder withStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
		return this;
	}
	
	public Builder type(String type) {
		this.type = type;
		return this;
	}
	
	public Builder title(String title) {
		this.title = title;
		return this;
	}
	
	public Builder instance(String instance) {
		this.instance = instance;
		return this;
	}
	
	public Builder addExtension(String key, Object value) {
		this.extensions.put(key, value);
		return this;
	}

		public Builder withDetails(String pattern, Object... arguments) {
			return withDetails(ParameterizedMessage.format(pattern, arguments));
		}

		public Builder withDetails(String errorDetails) {
			return withErrorDetails(errorDetails);
		}

		public ErrorTicket build() {
			return new ErrorTicket(this);
		}
	}

	@Override
public String toString() {
		return MoreObjects.toStringHelper(this).omitNullValues()
				.add("super", super.toString())
				.add("errorId", errorId)
				.add("errorDetails", errorDetails)
				.add("error", error)
				.add("errorGroup", errorGroup)
				.add("statusCode", statusCode)
				.add("type", type)
				.add("title", title)
				.add("instance", instance)
				.add("extensions", extensions)
				.toString();
	}
	
	class UndefinedErrorCode implements ErrorCode {
		private String name;
		private String group;
		public UndefinedErrorCode(String name, String group) {
			this.name = name;
		}
		@Override
		public String group() {
			return group;
		}
		
		@JsonValue
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
}
