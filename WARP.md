# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is **inqwise-error**, a Java library for standardized error handling and error transformation. The project implements a micro-kernel architecture with extensible error providers and transformers.

### Architecture Principles

- **Event-driven reactive architecture**: Error handling follows reactive patterns
- **Micro-kernel design with plugin-based providers**: Core error handling with specialized error transformers
- **Extensible error registry**: Built-in error handlers plus support for custom error types
- **Context-aware processing**: Error context management with metadata and stack traces
- **Flow control**: Support for error recovery and fallback strategies

## Build Commands

### Prerequisites
- Java 21+
- Maven 3.6.3+

### Common Maven Commands
```bash
# Compile the project
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ErrorHandlerTest

# Run a specific test method
mvn test -Dtest=ErrorHandlerTest#testBasicErrorHandling

# Create JAR
mvn package

# Install to local repository
mvn install

# Clean build artifacts
mvn clean
```

### Testing
- Uses JUnit 5 for unit testing
- Test resources located in `src/test/resources/`
- Integration tests demonstrate real-world usage patterns
- Test utilities in `TestUtils.java` provide common testing patterns

## Core Architecture

### Key Components

**ErrorHandler (Abstract Base)**
- Foundation class providing error registry, event handling, and transformation
- Manages delegation to specialized error handlers based on error types
- Handles error propagation and recovery callbacks

**Built-in Error Implementations**
- `StandardErrorHandler`: Handles common application errors with standardized formats  
- `ValidationErrorHandler`: Processes validation errors with field-level details
- `BusinessErrorHandler`: Manages domain-specific business rule violations

**Context Management**
- `ErrorContext`: Manages error state, metadata, and shared error properties
- `ErrorEvent`: Provides event information and control methods for handlers
- `ErrorTransformation`: Represents error transformation with type mapping
- `ErrorMetadata`: Wraps errors with additional context during processing

### Processing Flow
```
ErrorHandler.handle(error)
    ↓
Create ErrorContext  
    ↓
For each error handler:
    ↓
Generate ErrorEvent
    ↓
Call registered event handlers
    ↓
Apply error transformations
    ↓
Continue until handled/propagated
```

### Event-Driven Processing
- Register error handlers via `handler.errorHandler(event -> {...})`  
- Access error value: `event.error()`
- Access metadata: `event.meta().get(ErrorHandler.Keys.STACK_TRACE)`
- Control flow: `event.handled()`, `event.propagate()`, `event.recover(value)`
- Share data: `event.context().put(key, value)` and `event.context().get(key)`

## Development Patterns

### Creating Custom Error Handlers
When implementing new error handlers, extend `ErrorHandler` and implement:
- `type()`: Return the Class this handler processes
- `createErrorTransformer()`: Define how to transform the error
- Optional: Override `handleError()` for custom error handling logic

Example pattern from codebase:
```java
public class CustomErrorHandler extends ErrorHandler {
    @Override
    protected Class<?> type() {
        return MyCustomError.class;
    }
    
    @Override  
    protected ErrorTransformer createErrorTransformer(ErrorContext context) {
        return error -> {
            MyCustomError customError = (MyCustomError) error;
            return StandardError.builder()
                .code(customError.getCode())
                .message(customError.getMessage())
                .build();
        };
    }
}
```

### Error Handler Patterns
```java
errorHandler.handler(event -> {
    // Handler logic that might recover
    try {
        return alternativeOperation();
    } catch (Exception e) {
        event.propagate();
    }
});

errorHandler.finalHandler(context -> {
    System.err.println("Error: " + context.error().getMessage());
    System.err.println("Stack trace: " + context.get(ErrorHandler.Keys.STACK_TRACE));
});
```

## Dependencies

The library uses provided scope for most dependencies to avoid version conflicts:
- **Vert.x Core** (5.0.4): JSON processing support  
- **Google Guava** (33.4.0-jre): Collections utilities
- **Apache Log4j** (2.25.2): Logging framework
- **Inqwise Walker** (1.2.3): Object traversal support

## Project Rules & Conventions

Based on user rules, this project:
- Follows Inqwise's event-driven, reactive architecture principles
- Uses Vert.x 5 core tools
- Implements micro-kernel design with plugin-based provider modules
- Should commit changes on a daily basis for the entire project rather than after individual tasks

## Examples Location

Comprehensive usage examples will be available in:
- `src/main/java/com/inqwise/error/example/ErrorHandlingExample.java`

This file will demonstrate:
- Basic error handling
- Custom error handler implementation  
- Advanced error handling with context data
- Recovery patterns
- Error transformation patterns