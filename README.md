<p align="center">
  <img src="misc/logo-with-text.png" width="420px" alt="Flamingock logo" />
</p>

<h3 align="center">Execute Flamingock operations from the command line.</h3>

<p align="center">
  Apply changes, audit history, diagnose issues — before, after, or outside your application lifecycle.
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.flamingock/flamingock-cli"><img src="https://img.shields.io/maven-central/v/io.flamingock/flamingock-cli?label=Maven%20Central&color=blue" alt="Maven Central"/></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/></a>
</p>

---

## 🚀 Quick Start

```bash
# Apply pending changes
flamingock execute apply --jar ./my-app.jar

# Or run with the uber JAR
java -jar flamingock-cli-uber.jar execute apply --jar ./my-app.jar
```

---

## 🧩 What is Flamingock CLI?

Flamingock CLI is a command-line tool that lets you run [Flamingock](https://github.com/flamingock/flamingock-java) operations **outside your application's normal startup**. Instead of executing changes when your app boots, the CLI spawns your application JAR in a separate JVM process, runs the requested operation, and returns structured results.

This means you can:
- **Apply changes before deployment** — run changes in CI/CD pipelines, not during startup
- **Audit change history** — inspect what was applied, when, and by whom
- **Diagnose and fix issues** — get actionable guidance when something goes wrong
- **Preview changes safely** — dry-run to see what would be applied without side effects

Built for **DevOps engineers**, **Platform engineers**, **Administrators**, and **Developers** who need reliable, scriptable control over their Flamingock operations.

---

## 💡 Commands

### Operations

| Command         | Description                                             |
|-----------------|---------------------------------------------------------|
| `execute apply` | Apply pending changes                                   |
| `audit list`    | List audit entries (snapshot or full history)           |
| `audit fix`     | Fix a change's audit state (`APPLIED` or `ROLLED_BACK`) |
| `issue list`    | List changes with audit issues                          |
| `issue get`     | Get details and resolution guidance for an issue        |

### Global Options

| Option              | Description                                              |
|---------------------|----------------------------------------------------------|
| `--log-level`, `-l` | Application log level (`debug`, `info`, `warn`, `error`) |
| `--quiet`, `-q`     | Suppress non-essential output                            |
| `--no-color`        | Disable colored output                                   |
| `-J`, `--java-opt`  | JVM argument for the spawned process (repeatable)        |
| `--help`, `-h`      | Show help                                                |
| `--version`         | Show version                                             |

---

## 📋 Usage Examples

```bash
# Apply pending changes
flamingock execute apply --jar ./my-app.jar

# List current audit state
flamingock audit list --jar ./my-app.jar

# List full chronological audit history
flamingock audit list --jar ./my-app.jar --history

# List audit with extended details (execution ID, class, method, hostname)
flamingock audit list --jar ./my-app.jar --extended

# Filter audit entries since a date
flamingock audit list --jar ./my-app.jar --since 2025-01-01

# Fix a failed change
flamingock audit fix --jar ./my-app.jar -c user-change-id -r APPLIED

# List changes with issues
flamingock issue list --jar ./my-app.jar

# Get detailed guidance for a specific issue
flamingock issue get --jar ./my-app.jar -c user-change-id --guidance

# JSON output for CI/CD pipelines
flamingock issue list --jar ./my-app.jar --json

# Quiet mode for scripts
flamingock execute apply --jar ./my-app.jar --quiet

# Pass application arguments to the spawned JVM
flamingock execute apply --jar ./my-app.jar -- --spring.profiles.active=prod --spring.datasource.url=jdbc:mysql://prod/db

# Pass JVM arguments
flamingock execute apply --jar ./my-app.jar -J -Xmx512m -J -Xms256m

# Combine JVM and application arguments
flamingock audit list --jar ./my-app.jar -J -Xmx1g -- --spring.profiles.active=staging
```

---

## 🔑 Key Features

- **Process isolation** — Spawns your app in a separate JVM. No ClassLoader conflicts, no version mismatches, total isolation between the CLI and your application.

- **CI/CD friendly** — JSON output mode, proper stdout/stderr separation, standard exit codes, and non-interactive execution make it easy to integrate into any pipeline.

- **Actionable error messages** — Every error tells you what went wrong and how to fix it, with specific commands to run next.

- **Argument passthrough** — Pass application arguments (`--`) and JVM options (`-J`) to the spawned process. Reserved Flamingock flags are validated and protected.

- **GraalVM native image** — Build a standalone native binary with no JVM required. Fast startup, low memory footprint.

- **Comprehensive auditing** — Full chronological history, snapshot views, extended details, and date filtering for complete visibility into your change history.

---

## 📦 Installation

Choose the option that best fits your platform:

### Install script (recommended for most users)

#### Linux/WSL

```bash
curl -fsSL https://flamingock.io/cli/install/linux | bash

# Specific version or custom install directory (no sudo)
curl -fsSL https://flamingock.io/cli/install/linux | FLAMINGOCK_VERSION=1.1.0 FLAMINGOCK_INSTALL_DIR=~/.local/bin bash
```

#### macOS

```bash
curl -fsSL https://flamingock.io/cli/install/macos | bash

# Specific version or custom install directory (no sudo)
curl -fsSL https://flamingock.io/cli/install/macos | FLAMINGOCK_VERSION=1.1.0 FLAMINGOCK_INSTALL_DIR=~/.local/bin bash
```

#### Windows (PowerShell)

```powershell
irm https://flamingock.io/cli/install/win | iex

# Specific version
$env:FLAMINGOCK_VERSION="1.1.0"; irm https://flamingock.io/cli/install/win | iex
```

### macOS/Linux Homebrew

```bash
brew tap flamingock/tap
brew install flamingock
```

---

## 🏗️ Building from Source

### Prerequisites

- **Java 21** or later
- **GraalVM 21** (only for native image builds)

### Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Create uber JAR (fat JAR with all dependencies — runs as part of build)
./gradlew build
java -jar build/libs/flamingock-cli-*-uber.jar --help

# Build native image (requires GraalVM)
./gradlew nativeCompile
./build/native/nativeCompile/flamingock --help
```

### Integration Tests

End-to-end tests run the CLI against real MySQL databases using Docker:

```bash
# Build first, then run integration tests
./gradlew build
./integration-tests/run-tests.sh

# Test a native binary instead
CLI_CMD="./build/native/nativeCompile/flamingock" ./integration-tests/run-tests.sh

# Use a custom MySQL port
MYSQL_PORT=3308 ./integration-tests/run-tests.sh

# Use an existing MySQL instance (skip Docker)
MYSQL_HOST=myhost MYSQL_PORT=3306 ./integration-tests/run-tests.sh --no-docker
```

Run `./integration-tests/run-tests.sh --help` for all options.

---

## ⚙️ How It Works

```
┌─────────────────────┐         ┌──────────────────────────────────────┐
│   Flamingock CLI    │ spawns  │   User's App (spawned JVM)           │
│   (Picocli-based)   │────────►│                                      │
│                     │         │   --spring.main.web-application-     │
│   - Parse args      │◄────────│     type=none                        │
│   - Build command   │exit code│   --flamingock.cli.mode=true         │
│   - Launch JVM      │         │   --flamingock.operation=...         │
│   - Read result     │◄────────│   --flamingock.output-file=...       │
└─────────────────────┘  file   └──────────────────────────────────────┘
```

User-provided arguments are forwarded to the spawned process:
- **`-J` / `--java-opt`**: JVM arguments placed before `-jar`/`-cp` (e.g., `-J -Xmx512m`)
- **`--` separator**: Application arguments appended at the end (e.g., `-- --spring.profiles.active=prod`)

The CLI itself contains **no execution logic**. It is purely an orchestrator:

1. Creates a temporary file for the response
2. Spawns your application JAR with Flamingock flags
3. Your app executes the operation and writes the result
4. CLI reads, formats, and displays the result
5. Returns the appropriate exit code

---

## 🔢 Exit Codes

| Code  | Meaning                               |
|-------|---------------------------------------|
| `0`   | Success                               |
| `1`   | Execution error (change failed, etc.) |
| `2`   | Usage error (invalid arguments)       |
| `126` | JAR not found or not executable       |
| `130` | Interrupted (Ctrl+C)                  |

---

## 📘 Learn More

- [CLI Documentation](https://docs.flamingock.io/cli)
- [Flamingock Documentation](https://docs.flamingock.io)
- [Flamingock Java (main repo)](https://github.com/flamingock/flamingock-java)

---

## 🤝 Contributing

Contributions are welcome! Please check the [main repository](https://github.com/flamingock/flamingock-java) for contributing guidelines, or open an issue in this repo to report bugs and suggest improvements.

---

## 📜 License

Copyright 2026 Flamingock Contributors. Licensed under the [Apache License 2.0](LICENSE.md).
