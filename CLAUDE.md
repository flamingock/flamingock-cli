# CLAUDE.md

This file provides guidance to Claude Code when working with the Flamingock CLI codebase.

## Project Overview

**Flamingock CLI** is a command-line tool that enables executing Flamingock operations outside the normal application lifecycle. It spawns the user's application JAR in a separate JVM process with special flags, executes the requested operation, and returns structured results.

### Quality Standards

This CLI is designed to be a **professional, production-grade tool** with user experience on par with industry-leading CLIs like Docker, kubectl, and gh.

**Non-negotiable standards:**
- **Clear, actionable error messages**: Every error must tell the user what went wrong and how to fix it
- **Consistent output formatting**: Tables, colors, icons must be uniform across all commands
- **Proper exit codes**: Standard Unix conventions (0=success, 1=error, 2=usage, 130=interrupted)
- **CI/CD friendly**: JSON output mode, proper stdout/stderr separation, no interactive prompts in non-TTY
- **Graceful signal handling**: Ctrl+C must cleanly terminate child processes, no orphans
- **Comprehensive help**: Every command must have examples, descriptions, and proper --help
- **Robust error handling**: Bootstrap failures, timeouts, missing files - all must be handled gracefully
- **Professional logging**: Configurable log levels, quiet mode, no-color mode for accessibility

**Target audience expectations:**

| Role               | Expectation                                                   |
|--------------------|---------------------------------------------------------------|
| DevOps             | Reliable CI/CD integration, predictable behavior, JSON output |
| Platform Engineers | Clean exit codes, proper signal handling, scriptability       |
| Administrators     | Clear status reporting, actionable guidance for issues        |
| Developers         | Fast feedback, helpful error messages, dry-run capability     |

### What Flamingock Is

Flamingock is a platform for **audited, synchronized evolution of distributed systems**. It enables **Change-as-Code (CaC)**: all changes to external systems (schemas, configs, storage, etc.) are written as versioned, executable, auditable units of code.

The CLI allows DevOps, Platform Engineers, and Administrators to:
- Execute changes before deployment (not during startup)
- Validate that all changes are applied
- Dry-run to preview what changes would be applied
- Audit and fix change states
- Diagnose issues with detailed guidance

### Documentation

- **CLI Documentation**: https://docs.flamingock.io/cli
- **Main Flamingock Docs**: https://docs.flamingock.io

## Architecture

### Process-Based Execution

The CLI uses a **process-based architecture** - it spawns the user's application JAR in a separate JVM:

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

**Why process-based?**
- Total isolation between CLI and user's app
- Works with any Spring Boot version
- No ClassLoader complexity or version conflicts
- Simple to implement and debug

### Communication Flow

1. CLI creates a temporary JSON file for response
2. CLI spawns user's JAR with Flamingock flags
3. User's app executes the operation and writes result to the temp file
4. CLI reads the result and formats output
5. CLI returns appropriate exit code

## Supported Operations

| Operation          | Command                                                        | Description                           |
|--------------------|----------------------------------------------------------------|---------------------------------------|
| `EXECUTE_APPLY`    | `flamingock execute apply --jar ./app.jar`                     | Apply pending changes                 |
| `EXECUTE_VALIDATE` | `flamingock execute validate --jar ./app.jar`                  | Validate changes without applying     |
| `EXECUTE_DRYRUN`   | `flamingock execute dry-run --jar ./app.jar`                   | Preview what would be applied         |
| `EXECUTE_ROLLBACK` | `flamingock execute rollback --jar ./app.jar`                  | Rollback changes                      |
| `AUDIT_LIST`       | `flamingock audit list --jar ./app.jar`                        | List audit entries                    |
| `AUDIT_FIX`        | `flamingock audit fix --jar ./app.jar -c <id> -r <resolution>` | Fix a change's audit state            |
| `ISSUE_LIST`       | `flamingock issue list --jar ./app.jar`                        | List changes with issues              |
| `ISSUE_GET`        | `flamingock issue get --jar ./app.jar -c <id> --guidance`      | Get details and guidance for an issue |

All commands support `-- [APP_ARGS]` for passing application arguments and `-J <jvm-arg>` for JVM arguments to the spawned process.

## Module Structure

```
flamingock-cli-executor/
├── src/main/java/io/flamingock/cli/executor/
│   ├── FlamingockExecutorCli.java       # Main entry point
│   ├── command/
│   │   ├── ExecuteCommand.java          # Parent "execute" command
│   │   ├── ApplyCommand.java            # "apply" subcommand
│   │   ├── AuditCommand.java            # Parent "audit" command
│   │   ├── ListCommand.java             # "audit list" subcommand
│   │   ├── FixCommand.java              # "audit fix" subcommand
│   │   ├── IssueCommand.java            # Parent "issue" command
│   │   ├── ListIssueCommand.java        # "issue list" subcommand
│   │   └── GetIssueCommand.java         # "issue get" subcommand
│   ├── orchestration/
│   │   ├── CommandExecutor.java         # Command execution orchestration
│   │   ├── CommandResult.java           # Result model
│   │   └── ExecutionOptions.java        # Execution configuration
│   ├── process/
│   │   ├── JvmLauncher.java             # JVM process spawning
│   │   ├── JarTypeDetector.java         # Detects JAR type (Spring Boot, standalone)
│   │   ├── LaunchResult.java            # Launch result model
│   │   └── LaunchStatus.java            # Launch status enum
│   ├── output/
│   │   ├── ConsoleFormatter.java        # Terminal output formatting
│   │   ├── TableFormatter.java          # Table output
│   │   ├── JsonFormatter.java           # JSON output for CI/CD
│   │   ├── ExecutionResultFormatter.java
│   │   └── IssueFormatter.java          # Issue-specific formatting
│   ├── result/
│   │   └── ResponseResultReader.java    # Reads response from temp file
│   ├── handler/
│   │   └── ExecutorExceptionHandler.java
│   └── util/
│       └── VersionProvider.java
└── build.gradle.kts
```

## Technology Stack

- **CLI Framework**: Picocli 4.7.x
    - Declarative command annotations
    - Automatic help generation
    - Shell completion support
    - Professional colors and formatting

- **JSON Processing**: Jackson (for reading operation results)

- **Build**: Gradle with Kotlin DSL

- **Future**: GraalVM native image compilation for standalone executables

## Build Commands

```bash
# Build the project
./gradlew :cli:flamingock-cli-executor:build

# Run tests
./gradlew :cli:flamingock-cli-executor:test

# Create uber JAR (fat JAR with all dependencies)
./gradlew :cli:flamingock-cli-executor:shadowJar

# Run the CLI locally
java -jar cli/flamingock-cli-executor/build/libs/flamingock-cli-executor-<version>-uber.jar --help
```

## Key Design Patterns

### CommandExecutor Pattern

All commands delegate to `CommandExecutor` which handles the common flow:
1. Create temporary output file
2. Launch JVM with `JvmLauncher`
3. Handle launch failures
4. Read and parse response file
5. Clean up temporary files

Commands only handle their specific presentation logic.

### PassthroughArgsMixin Pattern

A Picocli `@Mixin` that captures user-provided passthrough arguments for the spawned JVM process. It handles:
- **`-J` / `--java-opt`** (repeatable `@Option`): JVM arguments placed before `-jar`/`-cp`
- **`APP_ARGS`** (`@Parameters` after `--`): Application arguments appended at the end of the spawned command
- **Reserved arg validation**: Rejects `--flamingock.*`, `--spring.main.web-application-type`, and `--spring.main.banner-mode` with actionable error messages

This mixin is included in all 5 leaf commands via `@Mixin private PassthroughArgsMixin passthroughArgs`.

### Result Formatting

- **Console mode**: Human-readable tables with colors and icons
- **JSON mode** (`--output json`): Machine-readable for CI/CD integration
- **Quiet mode** (`-q`): Minimal output, only essential information

### Exit Codes

| Code | Meaning                               |
|------|---------------------------------------|
| 0    | Success                               |
| 1    | Execution error (change failed, etc.) |
| 2    | Usage error (invalid arguments)       |
| 126  | JAR not found or not executable       |
| 130  | Interrupted (Ctrl+C)                  |

### Output Examples

**Successful execution:**
```
$ flamingock execute apply --jar ./my-app.jar

Flamingock CLI v1.0.0
────────────────────────────────────────

  ✓ Loading application... done (1.2s)
  ✓ Connected to MongoDB (localhost:27017)

Executing changes:
  ├─ [1/3] V001_CreateUsersTable ✓ (234ms)
  │        Author: john.doe
  │
  ├─ [2/3] V002_AddEmailIndex ✓ (89ms)
  │        Author: jane.smith
  │
  └─ [3/3] V003_SeedAdminUser ✓ (12ms)
           Author: john.doe

────────────────────────────────────────
SUCCESS │ 3 applied │ 1.5s
```

**Execution with error (actionable guidance):**
```
$ flamingock execute apply --jar ./my-app.jar

  ├─ [1/2] V001_CreateUsersTable ✓ (234ms)
  └─ [2/2] V002_MigrateUserData ✗ (1.2s)
           Error: NullPointerException at MigrateUserData.java:45

────────────────────────────────────────
FAILED │ 1 applied, 1 failed │ 1.8s

The change V002_MigrateUserData requires manual intervention.

To resolve this issue:
  1. Fix the underlying error in your change code
  2. Mark the change as resolved:
     flamingock audit fix --jar ./my-app.jar -c V002_MigrateUserData -r APPLIED

For detailed guidance:
  flamingock issue get --jar ./my-app.jar -c V002_MigrateUserData --guidance
```

**JSON output for CI/CD:**
```json
{
  "success": true,
  "durationMs": 1523,
  "changes": [
    {"id": "V001_CreateUsersTable", "status": "APPLIED", "durationMs": 234},
    {"id": "V002_AddEmailIndex", "status": "APPLIED", "durationMs": 89}
  ],
  "summary": {"total": 2, "applied": 2, "failed": 0, "skipped": 0}
}
```

## CLI Flags Passed to User's App

When the CLI spawns the user's JAR, it passes these flags:

```
--spring.main.web-application-type=none    # Disable web server
--flamingock.cli.mode=true                 # Enable CLI mode in Flamingock
--flamingock.operation=<OPERATION>         # The operation to execute
--flamingock.output-file=<temp-file>       # Where to write the result
```

Plus operation-specific arguments like:
- `--flamingock.change-id=<id>` for fix/get operations
- `--flamingock.resolution=<APPLIED|ROLLED_BACK>` for fix operation
- `--flamingock.audit.history=true` for audit list with history
- `--flamingock.guidance=true` for issue get with guidance

### Passthrough Arguments

Users can forward additional arguments to the spawned process:

- **`--` separator** for application arguments — appended at the END of the spawned command, after all Flamingock flags:
  ```
  flamingock execute apply --jar app.jar -- --spring.profiles.active=prod
  ```
- **`-J` / `--java-opt`** for JVM arguments — placed BEFORE `-jar`/`-cp` in the spawned command:
  ```
  flamingock execute apply --jar app.jar -J -Xmx512m -J "-Dmy.secret=value"
  ```

**Reserved arg validation**: The following prefixes are blocked from app args (after `--`) because they are controlled by the CLI:
- `--flamingock.*` — entire Flamingock namespace
- `--spring.main.web-application-type` — safety flag (CLI enforces `none`)
- `--spring.main.banner-mode` — CLI-controlled

Spring profiles (`--spring.profiles.active`), datasource URLs, and custom properties are NOT reserved — users need to control these. The CLI uses `--spring.profiles.include=flamingock-cli` (additive) so user profiles are safe.

## Relationship with flamingock-java

This CLI is designed to work with applications that include the Flamingock Java client library (`flamingock-core`). The core library:

1. Detects CLI mode via `--flamingock.cli.mode=true`
2. Parses the operation from `--flamingock.operation`
3. Executes the operation via `OperationFactory`
4. Writes the result to the output file
5. Exits with appropriate code

The CLI itself does NOT contain any change execution logic - it's purely an orchestrator that spawns the user's app.

## Terminology Guidelines

- Use **"changes"** not "migrations" - Flamingock is about system evolution, not just DB migrations
- The CLI is **"Flamingock CLI"** in user-facing content
- Currently only Spring Boot apps are supported, but the CLI is designed to be **framework-agnostic**

## Future Work

- **Distribution**: Homebrew, apt, yum, scoop packages (release artifacts are the foundation)
- **Additional frameworks**: Support for non-Spring Boot applications

## Development Guidelines

### Code Quality Requirements

This is a **professional tool used in production environments**. Every contribution must meet these standards:

1. **Error Handling**
    - Never show stack traces to users in normal operation
    - Every exception must be caught and translated to a user-friendly message
    - Include actionable guidance: "To fix this, try..."
    - Log detailed info at DEBUG level for troubleshooting

2. **Output Quality**
    - Console output must be visually clean and scannable
    - Use consistent spacing, alignment, and formatting
    - Colors should enhance readability, not distract
    - Respect `--no-color` and `--quiet` flags everywhere

3. **Testability**
    - All components must be unit testable with mocked dependencies
    - Integration tests for end-to-end flows
    - Test both success and failure paths
    - Test edge cases: empty results, large datasets, special characters

4. **Documentation**
    - Every public class and method must have Javadoc
    - Command help text must include examples
    - Update CLAUDE.md when adding new patterns or components

5. **Consistency**
    - Follow existing patterns in the codebase
    - Use the same formatting style (tables, headers, etc.)
    - Reuse existing components (ConsoleFormatter, TableFormatter, etc.)

### Error Message Guidelines

**Good error message:**
```
Error: JAR file not found: ./my-app.jar

To fix this:
  1. Check the file path is correct
  2. Ensure the file exists: ls -la ./my-app.jar
  3. Use an absolute path if needed

For more help: https://docs.flamingock.io/cli/troubleshooting
```

**Bad error message:**
```
FileNotFoundException: ./my-app.jar
```

### Adding New Features Checklist

- [ ] Command has clear `@Command` description and examples in footer
- [ ] All parameters have `description` and `paramLabel`
- [ ] Error cases return appropriate exit codes
- [ ] JSON output mode works correctly
- [ ] Quiet mode suppresses non-essential output
- [ ] Help text is complete and accurate
- [ ] Unit tests cover success and failure paths
- [ ] Documentation updated if needed

## Testing Approach

### Unit Tests

- Unit tests for individual components
- Mock-based tests for `JvmLauncher` and `ResponseResultReader`
- **Test all user-facing output** for consistency and clarity

```bash
./gradlew test
```

### Integration Tests

End-to-end tests that run the real CLI against real MySQL databases via Docker. Located in `integration-tests/`.

```
integration-tests/
├── docker-compose.yml          # MySQL 8 with configurable port
├── init-databases.sql          # Creates two isolated test databases
├── run-tests.sh                # Main test runner (chmod +x)
└── test-apps/                  # Test JARs (Spring Boot + standalone, MySQL-backed)
```

**Quick run:**
```bash
./gradlew build
./integration-tests/run-tests.sh
```

**Key options:**
- `--no-docker` — skip Docker, use an external MySQL instance
- `--verbose` — show full CLI output per test
- `CLI_CMD="./flamingock"` — test a native binary instead of the uber JAR
- `MYSQL_PORT=3308` — use a custom port to avoid conflicts

The script runs 8 tests (4 per JAR type: Spring Boot and standalone), verifying `execute apply`, `audit list`, `issue list`, and idempotent re-runs. Docker cleanup is handled automatically via trap, even on Ctrl+C.

## CI/CD

### PR Quality Gate (`quality_gate.yml`)

Triggers on PRs to `develop` and `workflow_dispatch`. Runs three jobs:
1. **Build & Unit Tests** — `./gradlew build`, uploads uber JAR artifact
2. **Native Image** — `./gradlew nativeCompile` on `ubuntu-latest`, uploads native binary
3. **Integration Tests** — downloads both artifacts, runs `run-tests.sh` against MySQL (Docker) for both uber JAR and native binary

### Release Workflow (`release.yml`)

Triggers on `workflow_dispatch` only (manual). Requires a `version` input (e.g. `1.0.1`) and an optional `dry_run` boolean. Four jobs:

1. **Validate** — reads version from the `version` input, validates semver format, warns if it doesn't match `gradle.properties`
2. **Build Native** (matrix: 3 targets) — builds native binaries for linux-x86_64, macos-arm64, windows-x86_64. Linux also uploads the uber JAR.
3. **Integration Test** — full MySQL integration tests on Linux (uber JAR + native binary), smoke tests for all artifacts (file format verification)
4. **Release** — collects all artifacts, generates `SHA256SUMS.txt`, creates git tag (`v<version>`), generates release notes via git-cliff, creates GitHub Release with `gh release create`. Pre-release auto-detected from version suffix (e.g. `1.0.1-beta.1`). Skipped on dry runs.

### Release Notes Generation

Release notes are generated automatically from **conventional commits** using [git-cliff](https://git-cliff.org/) (`orhun/git-cliff-action@v4`), configured in `cliff.toml` at the repo root. Commits are grouped into: Features, Bug Fixes, Refactor, Documentation, Performance, Testing, Miscellaneous, and Reverts.

**Range behavior:**
- **Stable releases** (e.g. `1.0.1`): aggregates all changes since the last stable tag, ignoring intermediate pre-release tags
- **Pre-releases** (e.g. `1.0.1-beta.1`): shows only changes since the immediately preceding tag

This requires commits to follow the [Conventional Commits](https://www.conventionalcommits.org/) format (e.g. `feat(cli): add dry-run mode`, `fix(process): handle SIGTERM`). Non-conventional commits are filtered out.

### How to Release

1. Update version in `gradle.properties`, commit and push
2. Go to **GitHub Actions → Release → "Run workflow"**
3. Enter the version (e.g. `1.0.2`, no `v` prefix)
4. Optionally check **dry_run** to test without creating a release

The workflow creates the git tag (`v1.0.2`) automatically during the release job.

### Release Artifacts

For version `X.Y.Z`, the GitHub Release contains:

| File | Description |
|------|-------------|
| `flamingock-X.Y.Z-linux-x86_64` | Native binary for Linux |
| `flamingock-X.Y.Z-macos-arm64` | Native binary for macOS Apple Silicon |
| `flamingock-X.Y.Z-windows-x86_64.exe` | Native binary for Windows |
| `flamingock-cli-X.Y.Z.jar` | Platform-independent uber JAR (requires JVM 21+) |
| `SHA256SUMS.txt` | SHA-256 checksums for all artifacts |

## Common Development Tasks

### Adding a New Command

1. Create command class in `command/` package with `@Command` annotation
2. Add as subcommand to parent command
3. Implement `Callable<Integer>` returning exit code
4. Use `CommandExecutor` for execution
5. Add appropriate formatter in `output/` package

### Adding a New Operation Type

1. Add enum value to `OperationType` in flamingock-core-commons
2. Add handling in `OperationFactory` in flamingock-core
3. Create CLI command that passes the new operation type
4. Create formatter for the result type

## Links

- **CLI Documentation**: https://docs.flamingock.io/cli
- **Main Documentation**: https://docs.flamingock.io
- **GitHub Issues**: https://github.com/flamingock/flamingock/issues
