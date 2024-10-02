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
	protected Integer statusCode;

	private ErrorTicket(Builder builder) {
		this.errorId = builder.errorId;
		this.errorDetails = builder.errorDetails;
		this.error = builder.error;
		this.errorGroup = builder.errorGroup;
		this.statusCode = builder.statusCode;
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
		
		if(null != error) {
			json.put(Keys.CODE, error.toString());
		}
		
		if(null != errorDetails) {
			json.put(Keys.DETAILS, errorDetails);
		}
		
		if(null != errorGroup) {
			json.put(Keys.ERROR_GROUP, errorGroup);
		}

		if(null != errorId) {
			json.put(Keys.ERROR_ID, errorId);
		}

		if(null != statusCode) {
			json.put(Keys.STATUS_CODE, statusCode);
		}

		return json;
	}
	
	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

	public Optional<Integer> optStatusCode(){
		return Optional.ofNullable(statusCode);
	}

	@Override
	public String getMessage() {
		return getErrorDetails();
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
		return MoreObjects.toStringHelper(this).omitNullValues().add("super", super.toString()).add("errorId", errorId)
				.add("errorDetails", errorDetails).add("error", error).add("errorGroup", errorGroup)
				.add("statusCode", statusCode).toString();
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
