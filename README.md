[![CI](https://github.com/inqwise/inqwise-errors/actions/workflows/ci.yml/badge.svg)](https://github.com/inqwise/inqwise-errors/actions/workflows/ci.yml)
[![Release](https://github.com/inqwise/inqwise-errors/actions/workflows/release.yml/badge.svg)](https://github.com/inqwise/inqwise-errors/actions/workflows/release.yml)
[![CodeQL](https://github.com/inqwise/inqwise-errors/actions/workflows/codeql.yml/badge.svg)](https://github.com/inqwise/inqwise-errors/actions/workflows/codeql.yml)
[![Snyk Security](https://github.com/inqwise/inqwise-errors/actions/workflows/snyk.yml/badge.svg)](https://github.com/inqwise/inqwise-errors/actions/workflows/snyk.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.inqwise/inqwise-errors.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.inqwise%22%20AND%20a:%22inqwise-errors%22)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![codecov](https://codecov.io/gh/inqwise/inqwise-errors/graph/badge.svg?token=YOUR_TOKEN)](https://codecov.io/gh/inqwise/inqwise-errors)
[![Java Version](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![RFC 6092](https://img.shields.io/badge/RFC-6092-green.svg)](https://tools.ietf.org/html/rfc6092)

# Inqwise Error Handling Library

## Overview
The `inqwise-errors` library provides structured error handling for complex applications. It introduces a set of classes to define error tickets, track error events, manage exceptions, and focus on stack traces in a consistent and maintainable manner. This library is particularly suited for building applications that require detailed error reporting, such as RESTful APIs, microservices, or enterprise software.

## Features
- **Error Ticket Creation**: Use the `ErrorTicket` class to capture error events, including details such as error codes, status codes, and optional exceptions.
- **Helper Functions for Error Checks**: The `ErrorTickets` helper class provides static methods for common error handling patterns, such as checking null references or validating conditions.
- **Exception Helpers**: `Throws` consolidates propagation, `NotFoundException`/`NotImplementedException` factories, and unboxing routines.
- **Bug Tracking**: Use the `Bug` class for representing unexpected issues that should never occur in a stable system.
- **Stack Trace Focusing**: Use `StackTraceFocuser` to filter stack trace elements and highlight the most relevant parts, making debugging easier.

> **Note:** The legacy `ExceptionNormalizer` flow has been removed and should not be used in new code paths.

## Getting Started
To use the library, include the following Maven dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>com.inqwise</groupId>
    <artifactId>inqwise-errors</artifactId>
    <version>${latest.version}</version>
</dependency>
```

## Defining Custom Error Codes and Providers
This section explains the steps to create custom error codes, define a provider for them, and correctly configure the library using SPI (Service Provider Interface). This is useful when you have a module with multiple custom error codes and want to ensure they are grouped and managed correctly.

### Step 1: Create the Custom Error Code Enum
Define your custom error codes using the `ErrorCode` interface and group them into a new enum class. For example, in a "Users" module:

```java
package com.example.errors;

import com.inqwise.errors.ErrorCode;

public enum UserErrors implements ErrorCode {
    UserNotExist,           // User does not exist in the system
    FirstNameIsMandatory,   // First name is a required field
    EmailInvalid;           // Email format is incorrect

    public static final String GROUP = "users";  // Define a group identifier

    @Override
    public String group() {
        return GROUP;
    }
}
```

### Step 2: Create a Provider for the Custom Error Code Enum
Create a class that implements the `ErrorCodeProvider` interface, using the `valueOf` method to retrieve the error code. This example follows the `ErrorCodesProvider` pattern by using `Enums.getIfPresent`:

```java
package com.example.errors;

import com.google.common.base.Enums;
import com.inqwise.errors.ErrorCode;
import com.inqwise.errors.spi.ErrorCodeProvider;

public class UserErrorCodeProvider implements ErrorCodeProvider {

    @Override
    public String group() {
        return UserErrors.GROUP;  // Return the group identifier for your custom error codes
    }

    @Override
    public ErrorCode valueOf(String errorCodeName) {
        // Use Enums.getIfPresent to safely retrieve the error code from the enum
        return Enums.getIfPresent(UserErrors.class, errorCodeName).orNull();
    }
}
```

### Step 3: Register the Provider Using SPI (Service Provider Interface)
Create a file named `com.inqwise.errors.spi.ErrorCodeProvider` inside the `src/main/resources/META-INF/services/` directory. This file should contain the fully qualified class name of your custom provider:

```
com.example.errors.UserErrorCodeProvider
```

**Directory Structure Example**:
```
src/
 └── main/
     └── resources/
         └── META-INF/
             └── services/
                 └── com.inqwise.errors.spi.ErrorCodeProvider
```

### Step 4: Use the Custom Errors in the Application
Once your custom error codes and provider are defined, you can use them to create error tickets in your application:

```java
import com.inqwise.errors.ErrorTicket;
import com.example.errors.UserErrors;

public class UserService {
    public void checkUserExists(String userId) {
        if (userId == null || userId.isEmpty()) {
            // Create an error ticket for the custom UserNotExist error
            ErrorTicket ticket = ErrorTicket.builder()
                                            .withError(UserErrors.UserNotExist)
                                            .withErrorDetails("User ID cannot be null or empty.")
                                            .build();
            System.err.println("Error: " + ticket.toString());
        }
    }
}
```

### Step 5: Verify the Provider and Errors
To ensure that your custom errors are registered correctly, you can query the `ErrorCodeProviders` utility and retrieve a specific error code using its string representation:

```java
import com.inqwise.errors.ErrorCodeProviders;
import com.example.errors.UserErrors;

public class Test {
    public static void main(String[] args) {
        var provider = ErrorCodeProviders.get(UserErrors.GROUP);
        System.out.println("Error Code Class: " + provider.valueOf(UserErrors.UserNotExist.toString()).getClass());
    }
}
```

## Error Management Classes Overview
The following sections describe the main classes and functionalities for error handling and management in this library.

### `ErrorTicket`
`ErrorTicket` encapsulates structured problem details (RFC 7807) and OAuth-compatible payloads. Builders support fluent configuration of `type`, `title`, `instance`, status, and custom extension fields to encode actionable metadata.

**Example: Building and serializing an ErrorTicket**
```java
ErrorTicket ticket = ErrorTicket.builder()
    .withError(UserErrors.FirstNameIsMandatory)
    .withErrorGroup(UserErrors.GROUP)
    .withStatusCode(400)
    .type("https://errors.example.com/users/first-name")
    .title("First Name Missing")
    .instance("/api/users/42")
    .addExtension("hint", "Provide the firstName field")
    .build();

JsonObject payload = ticket.toJson();
```

Implementing `ProvidesErrorTicket` in your own exceptions lets consumers call `toErrorTicket()` for consistent serialization/logging, while still exposing the underlying `Builder` for customization.

### `ErrorTickets`
`ErrorTickets` provides guard utilities (e.g., `checkNotNull`, `checkArgument`, `checkAnyNotNull`) that throw consistent `ErrorTicket`s. These helpers centralize validation while keeping business logic concise.

**Example: Guarding request payloads**
```java
void validate(UserRequest req) {
    ErrorTickets.checkNotNull(req, "Request cannot be null");
    ErrorTickets.checkArgument(req.getEmail().contains("@"), builder ->
        builder.withError(ErrorCodes.ArgumentWrong).withErrorDetails("Invalid email"));
}
```

### `Bug`
The `Bug` class is used to represent unexpected conditions that should never happen in a properly functioning application.

**Example: Using the `Bug` Class**
```java
import com.inqwise.errors.Bug;

public class SystemValidator {
    public void validateSystemState() {
        boolean unexpectedCondition = true;
        if (unexpectedCondition) {
            throw new Bug("An unexpected condition occurred: {}", "Invalid system state");
        }
    }
}
```

### `Throws`
`Throws` centralizes exception helpers: quickly propagate checked exceptions, build `NotFoundException`/`NotImplementedException`, or unwrap nested wrappers.

**Example: Unwrapping nested completion exceptions**
```java
try {
    future.join();
} catch (CompletionException e) {
    throw Throws.propagate(Throws.unbox(e, CompletionException.class));
}
```

### `StackTraceFocuser`
`StackTraceFocuser` trims stack traces by cloning exceptions and removing frames that match configured predicates. Use `StackTraceFocuser.defaultInstance()` for the built-in JDK-filtering variant or the fluent builder to specify class, method, and file patterns.

**Example: Custom Stack Trace Filtering**
```java
import com.inqwise.errors.StackTraceFocuser;

public class CustomStackTraceFocusing {
    public static void main(String[] args) {
        try {
            throw new RuntimeException("Simulated custom exception");
        } catch (Exception e) {
            StackTraceFocuser focuser = StackTraceFocuser.builder()
                .addClass("^java\\.")                 // ignore stdlib classes
                .addClass("^org\\.junit\\.")        // ignore test harness
                .addMethod("^lambda$")                // strip synthetic lambdas
                .addFile(".*Proxy\\.java")           // strip generated proxies
                .build();

            Throwable focusedException = focuser.apply(e);
            focusedException.printStackTrace();
        }
    }
}
```

## License
This library is licensed under the MIT License. See the `LICENSE` file for more details.

---
