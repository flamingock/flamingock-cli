# Flamingock CLI

This is the standalone Flamingock CLI repository, extracted from the flamingock-java monorepo.

## Overview

The Flamingock CLI is a command-line tool for executing and managing changes in applications. It provides commands for:

- **apply** - Apply pending changes to the target system
- **list** - List all changes and their status
- **audit** - View audit history
- **issue** - Manage issues (list, get, fix)

## Build System

This is a single-module Gradle project using Kotlin DSL with Java 21 (GraalVM).

### Requirements

- **Java 21** (GraalVM recommended for native image support)
- **SDKMAN** (optional, for automatic Java version switching)

If using SDKMAN, run `sdk env install` to automatically install the correct Java version.

### Common Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Create UberJar (self-contained executable)
./gradlew uberJar

# Run the CLI
java -jar build/libs/flamingock-cli-1.0.1-uber.jar --help

# Check license headers
./gradlew spotlessCheck

# Fix license headers
./gradlew spotlessApply

# Clean build
./gradlew clean build
```

### Running the CLI

After building, run the CLI with:

```bash
# Show help
java -jar build/libs/flamingock-cli-1.0.1-uber.jar --help

# Apply changes to an application JAR
java -jar build/libs/flamingock-cli-1.0.1-uber.jar apply -j /path/to/app.jar

# List changes
java -jar build/libs/flamingock-cli-1.0.1-uber.jar list -j /path/to/app.jar
```

## Architecture

### Package Structure

- `io.flamingock.cli.executor` - Main CLI entry point
- `io.flamingock.cli.executor.command` - CLI commands (Apply, List, Audit, Issue)
- `io.flamingock.cli.executor.orchestration` - Command execution orchestration
- `io.flamingock.cli.executor.process` - JVM process launching and JAR detection
- `io.flamingock.cli.executor.output` - Output formatting (Table, JSON)
- `io.flamingock.cli.executor.result` - Response parsing
- `io.flamingock.cli.executor.handler` - Exception handling
- `io.flamingock.cli.executor.util` - Utilities (version provider)

### Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `io.flamingock:flamingock-core-commons` | 1.0.1 | OperationType enum, response models |
| `io.flamingock:general-util` | 1.0.1 | General utilities |
| `info.picocli:picocli` | 4.7.5 | CLI framework |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.16.0 | Date/time JSON |

## Terminology

- Use "**changes**" not "migrations" - Flamingock is about system evolution, not just database migrations
- The CLI is **framework-agnostic** - currently supports Spring Boot applications, with more frameworks planned

## License

Apache License 2.0 - See LICENSE file for details.

## License Header Management

All Java source files must include the Flamingock license header.

```bash
# Check headers
./gradlew spotlessCheck

# Fix headers
./gradlew spotlessApply
```

## Future: GraalVM Native Image

This project is configured for Java 21 (GraalVM) to enable future native image compilation for standalone executables that don't require a JVM.
