package com.inqwise.errors;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import org.apache.logging.log4j.message.ParameterizedMessage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Enums;
import com.google.common.base.MoreObjects;
import com.inqwise.errors.spi.ErrorCodeProvider;

import io.vertx.core.json.JsonObject;

/**
 * Serializable wrapper around a structured error payload compatible with RFC 7807.
 */
@JsonAutoDetect(getterVisibility=Visibility.NONE, isGetterVisibility = Visibility.NONE)
public class ErrorTicket extends RuntimeException {
	
	/**
	* 
	*/
	private static final long serialVersionUID = 3636672196437415424L;
	
	private final static int UNIQUE_IDENTIFIER = 2057221941;

	/**
	* Returns a deterministic identifier useful for schema evolution guards.
	*
	* @return unique identifier
	*/
	public static int getUniqueIdentifier() {
		return UNIQUE_IDENTIFIER;
	}

	/** Field names used when serializing {@link ErrorTicket}. */
	public static class Keys {
		/** Public constructor for reflective/test usage. */
		public Keys() {}
		/** Error code field name. */
		public static final String CODE = "code";
		/** Error group field name. */
		public static final String ERROR_GROUP = "group";
		/** Error id field name. */
		public static final String ERROR_ID = "id";
		
		// RFC 7807 standard fields
		/** Type URI field name. */
		public static final String TYPE = "type";
		/** Title field name. */
		public static final String TITLE = "title";
		/** Status code field name. */
		public static final String STATUS = "status";
		/** Alias for {@link #STATUS}. */
		public static final String STATUS_CODE = STATUS;
		/** Detail field name. */
		public static final String DETAIL = "detail";
		/** Instance field name. */
		public static final String INSTANCE = "instance";
		
		// OAuth 2.0 fields (RFC 6749)
		/** OAuth error field. */
		public static final String ERROR = "error";
		/** OAuth error description field. */
		public static final String ERROR_DESCRIPTION = "error_description";
		/** OAuth error URI field. */
		public static final String ERROR_URI = "error_uri";
	}
	
	@Deprecated(forRemoval = true)
	void setErrorId(String errorId) {
		this.errorId = errorId;
	}
	
	@Deprecated(forRemoval = true)
	void setError(String error) {
		this.errorId = error;
	}

	@Deprecated(forRemoval = true)
	void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	@Deprecated(forRemoval = true)
	void setErrorGroup(String group) {
		this.errorGroup = group;
	}
	
	/** Unique identifier correlating errors across systems. */
	@JsonProperty(Keys.ERROR_ID)
	protected String errorId;
	/** Human-readable description of the failure. */
	@JsonProperty(Keys.DETAIL)
	protected String errorDetails;
	/** Strongly typed error classification. */
	@JsonProperty(Keys.CODE)
	protected ErrorCode error;
	/** Group identifier used to resolve {@link ErrorCodeProvider}. */
	@JsonProperty(Keys.ERROR_GROUP)
	protected String errorGroup;
	/** HTTP status code suggestion. */
	@JsonProperty(Keys.STATUS)
	private Integer statusCode;
	
	// RFC 7807 fields
	/** RFC 7807 type URI describing the error class. */
	@JsonProperty(Keys.TYPE)
	protected String type;
	/** Short human-readable summary of the failure. */
	@JsonProperty(Keys.TITLE)
	protected String title;
	/** Reference URI identifying this specific occurrence. */
	@JsonProperty(Keys.INSTANCE)
	protected String instance;
	
	/** Additional metadata appended as RFC 7807 extensions. */
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

	/**
	* Rehydrates an {@link ErrorTicket} from a {@link Map} representation, typically originating
	* from Jackson or Vert.x decoding.
	*
	* @param map payload map
	* @return reconstituted ticket
	*/
	@JsonCreator
	public static ErrorTicket fromMap(Map<String,Object> map) {
		return new ErrorTicket(new JsonObject(map));
	}
	
	/**
	* Convenience overload of {@link #parse(JsonObject, String)} with no default group.
	*
	* @param json serialized ticket
	* @return parsed ticket
	*/
	public static final ErrorTicket parse(JsonObject json) {
		return parse(json, null);
	}
	
	/**
	* Parses a Vert.x {@link JsonObject} into an {@link ErrorTicket}, optionally supplying a
	* fallback group when none is present in the payload.
	*
	* @param json serialized ticket
	* @param defaultGroup fallback group when not embedded
	* @return parsed ticket
	*/
	public static final ErrorTicket parse(JsonObject json, String defaultGroup) {
		var builder = ErrorTicket.builder();
		builder.withErrorDetails(json.getString(Keys.DETAIL));
		builder.withErrorId(json.getString(Keys.ERROR_ID));
		builder.withStatusCode(json.getInteger(Keys.STATUS));
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
	
	/**
	* Builds an {@link ErrorTicket} from a raw {@link JsonObject}, preserving unknown extensions.
	*
	* @param json serialized ticket
	*/
	public ErrorTicket(JsonObject json) {
		this.errorDetails = json.getString(Keys.DETAIL);
		this.errorId = json.getString(Keys.ERROR_ID);
		this.statusCode = json.getInteger(Keys.STATUS);
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
		this.type = json.getString(Keys.TYPE);
		this.title = json.getString(Keys.TITLE);
		this.instance = json.getString(Keys.INSTANCE);
		this.extensions = json.getJsonObject(errorCodeName, new JsonObject()).getMap();
	}
	
	/**
	* Returns the raw {@link ErrorCode} cast without type-safety (use sparingly).
	*
	* @param <T> error type
	* @return casted error
	*/
	public <T extends ErrorCode> T getErrorUnsafe() {
		return (T)error;
	}
	
	/**
	* Returns the associated {@link ErrorCode}, or {@code null} if not set.
	*
	* @return current error
	*/
	public ErrorCode getError() {
		return error;
	}
	
	/**
	* Returns the error cast to the requested type.
	*
	* @param <T> target type
	* @param type class representing the target type
	* @return casted error
	*/
	public <T extends ErrorCode> T getError(Class<T> type) {
		Objects.requireNonNull(type);
		return type.cast(error);
	}
	
	/**
	* Returns the stored error when it matches the provided type.
	*
	* @param type the target error type
	* @param <T> the error subtype
	* @return the casted error or {@code null}
	*/
	public <T extends ErrorCode> T optError(Class<T> type) {
		Objects.requireNonNull(type);
		if (error != null && type.isInstance(error)) {
			return getError(type);
		}
		return null;
	}
	
	/**
	* Returns the error as an {@link ErrorCodes} enum, defaulting to {@link ErrorCodes#GeneralError}.
	*
	* @return resolved {@link ErrorCodes} value
	*/
	public ErrorCodes getErrorAsErrorCodes() {
		if(null != error && error instanceof ErrorCodes) {
			return (ErrorCodes)error;
		} else {
			return ErrorCodes.GeneralError;
		}
	}
	
	/**
	* Returns the unique identifier assigned to the ticket.
	*
	* @return ticket id
	*/
	public String getErrorId() {
		return errorId;
	}

	/**
	* Returns the human-readable detail string.
	*
	* @return detail text
	*/
	public String getErrorDetails(){
		return errorDetails;
	}
	
	/**
	* Returns the group identifier used to resolve {@link ErrorCodeProvider}s.
	*
	* @return error group name
	*/
	public String getErrorGroup() {
		return errorGroup;
	}

	/**
	* Checks that the stored error does not match any excluded codes.
	*
	* @param notErrors the codes to skip
	* @return {@code true} when the stored error is not in the exclusion list
	*/
	public boolean hasErrorExcept(ErrorCode... notErrors){
		return null != notErrors && !Arrays.stream(notErrors).anyMatch((itm) -> itm == error);
	}

	/**
	* Checks whether the stored error matches any of the provided codes.
	*
	* @param errors acceptable codes
	* @return {@code true} when a match is found
	*/
	public boolean hasError(ErrorCode... errors){
		return null != errors && Arrays.stream(errors).anyMatch((itm) -> itm == error);
	}

	/**
	* Serializes the ticket to a {@link JsonObject} containing RFC 7807 / OAuth properties.
	*
	* @return immutable JSON payload
	*/
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
			json.put(Keys.DETAIL, errorDetails); // RFC 7807 field
		}
		
		if(null != errorGroup) {
			json.put(Keys.ERROR_GROUP, errorGroup);
		}

		if(null != errorId) {
			json.put(Keys.ERROR_ID, errorId);
		}

		if(null != statusCode) {
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
	
	/**
	* @deprecated prefer {@link Builder#withStatusCode(Integer)}
	* @param statusCode HTTP status to set
	*/
	@Deprecated(forRemoval = true)
	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	* Gets the media type for this error response.
	*
	* <p>For RFC 7807 Problem Details, returns {@code application/problem+json}. For OAuth 2.0
	* errors, returns {@code application/json}.</p>
	*
	* @return MIME type
	*/
	public String getContentType() {
		return type != null && !"oauth".equals(errorGroup) ? "application/problem+json" : "application/json";
	}
	
	/**
	* Returns response headers that should accompany this error.
	*
	* @return header map (possibly empty)
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
	* Indicates whether localization is supported.
	*
	* @return {@code true} when localized messages may be provided
	*/
	public boolean supportsLocalization() {
		return true; // Base implementation supports i18n
	}

	/**
	* Returns the HTTP status code when present.
	*
	* @return optional status
	*/
	public Optional<Integer> optStatusCode(){
		return Optional.ofNullable(statusCode);
	}

	/**
	* Gets the HTTP status code for this error.
	*
	* @return HTTP status or {@code null}
	*/
	public Integer getStatus() {
		return statusCode;
	}

	/**
	* @return message combining code and detail for logging.
	*/
	@Override
	public String getMessage() {
		return String.format("%s:%s", getErrorUnsafe(), getErrorDetails());
	}

	/**
	* Normalizes {@link Throwable}s into {@link ErrorTicket}s, optionally mutating via callback.
	*
	* @param t the throwable to convert
	* @param creator optional callback to customize the builder
	* @return resulting {@link ErrorTicket}
	*/
	public static ErrorTicket propagate(Throwable t, Consumer<ErrorTicket.Builder> creator){
		
		ErrorTicket.Builder builder;
		
		if(t instanceof ErrorTicket){
			builder = builderFrom((ErrorTicket)t);
		}else if(t instanceof ProvidesErrorTicket) {
			builder = ((ProvidesErrorTicket) t).getErrorTicketBuilder();
		} else {
			builder = builder().withError(ErrorCodes.GeneralError);
			if(null == t.getMessage()) {
				if(t instanceof NullPointerException) {
					builder.withDetails("NullPointer");
				} else {
					builder.withDetails(t.getClass().getName());
				}
			} else {
				builder.withDetails("{}:{}", t.getClass().getName(), t.getMessage());
			}
		}
		
		if(null != creator) { 
			creator.accept(builder);
		}
		return builder.build();
	}

	/**
	* Variant of {@link #propagate(Throwable, Consumer)} without mutation callback.
	*
	* @param t the throwable to convert
	* @return resulting {@link ErrorTicket}
	*/
	public static ErrorTicket propagate(Throwable t) {
		return propagate(t, null);
	}

	/**
	* Creates a fresh {@link Builder} instance with a generated identifier.
	*
	* @return new builder
	*/
	public static Builder builder() {
		return new Builder();
	}

	/**
	* Seeds a builder with values from an existing ticket.
	*
	* @param errorTicket the source ticket
	* @return initialized builder
	*/
	public static Builder builderFrom(ErrorTicket errorTicket) {
		return new Builder(errorTicket);
	}

	/**
	* Fluent builder used to create {@link ErrorTicket} instances with consistent defaults.
	*/
	public static final class Builder {
		private static final char[] CONSONANTS = "bcdfghjkmnpqrstvwxyz".toCharArray();
		private static final char[] VOWELS = "aeiou".toCharArray();
		private static final char[] DIGITS = "23456789".toCharArray();
		private static final int DIGIT_PROBABILITY = 5; // ~20%
	
		private static final Random RND = new Random();
		private String errorId = null;
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
		
		private static String generate(int size, boolean preventFirstCharDigit) {
			StringBuilder sb = new StringBuilder(size);
			
			// Add the prefix if specified
			
			int startIndex = sb.length();
			int remainingSize = size - startIndex;

			for (int i = 0; i < remainingSize; i++) {
				int absolutePosition = startIndex + i;
				
				if (absolutePosition % 2 == 0) {
					// Even positions: consonants or digits
					boolean shouldUseDigit = RND.nextInt(DIGIT_PROBABILITY) == 0;
					
					// Prevent first character from being a digit if configured
					if (preventFirstCharDigit && absolutePosition == 0 && shouldUseDigit) {
						shouldUseDigit = false;
					}
					
					if (shouldUseDigit) {
						sb.append(randomChar(DIGITS));
					} else {
						sb.append(randomChar(CONSONANTS));
					}
				} else {
					// Odd positions: vowels
					sb.append(randomChar(VOWELS));
				}
			}
			return sb.toString();
		}
		
		private static char randomChar(char[] source) {
			return source[RND.nextInt(source.length)];
		}
		
		private Builder() {
			this.errorId = "et".concat(generate(7, false));
		}

		private Builder(ErrorTicket errorTicket) {
			this.errorId = errorTicket.errorId;
			this.errorDetails = errorTicket.errorDetails;
			this.error = errorTicket.error;
			this.errorGroup = errorTicket.errorGroup;
			this.statusCode = errorTicket.statusCode;
			this.instance = errorTicket.instance;
			this.type = errorTicket.type;
			this.title = errorTicket.title;
			if(null != errorTicket.extensions) {
				this.extensions.putAll(errorTicket.extensions);
			}
		}

		/**
		* Overrides the generated error identifier.
		*
		* @param errorId explicit identifier
		* @return this builder
		*/
		public Builder withErrorId(String errorId) {
			this.errorId = errorId;
			return this;
		}

		/**
		* Sets human-readable details.
		*
		* @param errorDetails detail string
		* @return this builder
		*/
		public Builder withErrorDetails(String errorDetails) {
			this.errorDetails = errorDetails;
			return this;
		}

		/**
		* Assigns the {@link ErrorCode} for the ticket.
		*
		* @param error code to set
		* @return this builder
		*/
		public Builder withError(ErrorCode error) {
			this.error = error;
			return this;
		}

		/**
		* Specifies the error group used to resolve providers.
		*
		* @param errorGroup provider group name
		* @return this builder
		*/
		public Builder withErrorGroup(String errorGroup) {
			this.errorGroup = errorGroup;
			return this;
		}

		/**
		* Sets the HTTP status code.
		*
		* @param statusCode HTTP status to emit
		* @return this builder
		*/
		public Builder withStatusCode(Integer statusCode) {
			this.statusCode = statusCode;
			return this;
		}
		
		/**
		* Sets the RFC 7807 {@code type} URI.
		*
		* @param type canonical type URI
		* @return this builder
		*/
		public Builder wIthType(String type) {
			this.type = type;
			return this;
		}

		/**
		* Alias for {@link #wIthType(String)} for fluent DSL compatibility.
		*
		* @param type canonical type URI
		* @return this builder
		*/
		public Builder type(String type) {
			return wIthType(type);
		}
		
		/**
		* Sets the RFC 7807 {@code title} field.
		*
		* @param title short description
		* @return this builder
		*/
		public Builder withTitle(String title) {
			this.title = title;
			return this;
		}

		/**
		* Alias for {@link #withTitle(String)} for fluent DSL compatibility.
		*
		* @param title short description
		* @return this builder
		*/
		public Builder title(String title) {
			return withTitle(title);
		}
		
		/**
		* Sets the RFC 7807 {@code instance} identifier.
		*
		* @param instance occurrence URI
		* @return this builder
		*/
		public Builder withInstance(String instance) {
			this.instance = instance;
			return this;
		}

		/**
		* Alias for {@link #withInstance(String)} for fluent DSL compatibility.
		*
		* @param instance occurrence URI
		* @return this builder
		*/
		public Builder instance(String instance) {
			return withInstance(instance);
		}
		
		/**
		* Adds a custom extension field.
		*
		* @param key extension name
		* @param value extension value
		* @return this builder
		*/
		public Builder addExtension(String key, Object value) {
			this.extensions.put(key, value);
			return this;
		}

		/**
		* Formats the detail string via {@link ParameterizedMessage}.
		*
		* @param pattern log4j parameterized pattern
		* @param arguments arguments for the pattern
		* @return this builder
		*/
		public Builder withDetails(String pattern, Object... arguments) {
			return withDetails(ParameterizedMessage.format(pattern, arguments));
		}

		/**
		* Sets the detail string directly.
		*
		* @param errorDetails literal detail text
		* @return this builder
		*/
		public Builder withDetails(String errorDetails) {
			return withErrorDetails(errorDetails);
		}

		/**
		* Creates the {@link ErrorTicket}, inferring status from the {@link ErrorCode} when omitted.
		*
		* @return immutable {@link ErrorTicket}
		*/
		public ErrorTicket build() {
			if(null == statusCode && null != error && 0 != error.statusCode()) {
				statusCode = error.statusCode();
			}
			return new ErrorTicket(this);
		}
	}

	/**
	* @return diagnostic string containing all populated fields.
	*/
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
	
	private static final boolean isDigitsNoLeadingZeros(String s) {
		if (s == null || s.isEmpty()) return false;

		int len = s.length();

		// first digit cannot be zero
		if (s.charAt(0) == '0') {
			return false;
		}

		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}

		return true;
	}
	
	class UndefinedErrorCode implements ErrorCode {
		private String name;
		private String group;
		private int statusCode = 0;
		/**
		* @param name unresolved error name from payload
		* @param group originating error group (nullable)
		*/
		public UndefinedErrorCode(String name, String group) {
			this.name = name;
			this.group = group;
			if(isDigitsNoLeadingZeros(name)) {
				statusCode = Integer.parseInt(name);
			}
		}
		/** @return fallback group for unresolved codes. */
		@Override
		public String group() {
			return group;
		}
		
		/**
		* @return unresolved error name for serialization.
		*/
		@JsonValue
		public String getName() {
			return name;
		}
		
		/**
		* @return name for debugging/logging.
		*/
		@Override
		public String toString() {
			return name;
		}
		
		/**
		* @return parsed numeric status if name is digits, otherwise zero.
		*/
		public int getStatusCode() {
			return statusCode;
		}
		
	}
}
