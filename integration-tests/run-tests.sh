#!/usr/bin/env bash
#
# Flamingock CLI — Integration Test Runner
#
# Runs end-to-end tests against real MySQL databases using Docker.
# Works with both the JVM uber JAR and native binaries.
#
# Usage:
#   ./integration-tests/run-tests.sh                  # default (auto-detect uber JAR)
#   CLI_CMD="./flamingock" ./integration-tests/run-tests.sh   # test native binary
#   MYSQL_PORT=3308 ./integration-tests/run-tests.sh          # custom MySQL port
#   ./integration-tests/run-tests.sh --no-docker              # skip Docker (use external MySQL)
#   ./integration-tests/run-tests.sh --verbose                # show full CLI output
#
set -euo pipefail

# ─── Resolve paths ────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ─── Configuration (override via environment) ─────────────────────────────────

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USER:-flamingock}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-flamingock_pass}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-flamingock_root}"
SKIP_DOCKER="${SKIP_DOCKER:-false}"
TEST_TIMEOUT="${TEST_TIMEOUT:-120}"
VERBOSE="${VERBOSE:-false}"

# ─── Colors & formatting ──────────────────────────────────────────────────────

if [[ -t 1 ]]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[0;33m'
    CYAN='\033[0;36m'
    BOLD='\033[1m'
    RESET='\033[0m'
else
    RED='' GREEN='' YELLOW='' CYAN='' BOLD='' RESET=''
fi

# ─── Helpers ──────────────────────────────────────────────────────────────────

info()  { echo -e "${CYAN}[INFO]${RESET}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error() { echo -e "${RED}[ERROR]${RESET} $*" >&2; }
pass()  { echo -e "  ${GREEN}PASS${RESET} $*"; }
fail()  { echo -e "  ${RED}FAIL${RESET} $*"; }

usage() {
    cat <<EOF
Flamingock CLI Integration Tests

Usage: $(basename "$0") [OPTIONS]

Options:
  --no-docker    Skip docker-compose up/down (use existing MySQL)
  --verbose      Show full CLI output for each test
  --help         Show this help message

Environment variables:
  CLI_CMD            CLI command (default: auto-detect uber JAR)
  MYSQL_HOST         MySQL host (default: 127.0.0.1)
  MYSQL_PORT         MySQL mapped port (default: 3307)
  MYSQL_USER         MySQL user (default: flamingock)
  MYSQL_PASSWORD     MySQL password (default: flamingock_pass)
  SKIP_DOCKER        Skip Docker management (default: false)
  TEST_TIMEOUT       Per-test timeout in seconds (default: 120)

Examples:
  ./integration-tests/run-tests.sh
  MYSQL_PORT=3308 ./integration-tests/run-tests.sh
  CLI_CMD="./flamingock" ./integration-tests/run-tests.sh --no-docker
EOF
    exit 0
}

# ─── Parse CLI flags ──────────────────────────────────────────────────────────

for arg in "$@"; do
    case "$arg" in
        --no-docker) SKIP_DOCKER=true ;;
        --verbose)   VERBOSE=true ;;
        --help|-h)   usage ;;
        *)           error "Unknown option: $arg"; usage ;;
    esac
done

# ─── Detect timeout command ───────────────────────────────────────────────────

TIMEOUT_CMD=""
if command -v timeout &>/dev/null; then
    TIMEOUT_CMD="timeout"
elif command -v gtimeout &>/dev/null; then
    TIMEOUT_CMD="gtimeout"
else
    warn "Neither 'timeout' nor 'gtimeout' found. Tests will run without timeout."
    warn "Install coreutils for timeout support: brew install coreutils (macOS)"
fi

# ─── Auto-detect CLI command ──────────────────────────────────────────────────

if [[ -z "${CLI_CMD:-}" ]]; then
    UBER_JAR=$(find "${PROJECT_ROOT}/build/libs" -name "flamingock-cli-*-uber.jar" -print -quit 2>/dev/null || true)
    if [[ -n "$UBER_JAR" ]]; then
        CLI_CMD="java -jar ${UBER_JAR}"
    else
        error "No uber JAR found in build/libs/"
        error "Build it first: ./gradlew build"
        exit 2
    fi
fi

# ─── Test JAR paths ───────────────────────────────────────────────────────────

SPRINGBOOT_JAR="${SCRIPT_DIR}/test-apps/test-app-flamingock-1.0.1-springboot-mysql.jar"
STANDALONE_JAR="${SCRIPT_DIR}/test-apps/test-app-flamingock-1.0.1-standalone-mysql.jar"

# ─── Precondition checks ─────────────────────────────────────────────────────

check_preconditions() {
    local ok=true

    # Check CLI command works
    local cli_first_word
    cli_first_word=$(echo "$CLI_CMD" | awk '{print $1}')
    if [[ "$cli_first_word" == "java" ]]; then
        if ! command -v java &>/dev/null; then
            error "java not found in PATH"
            ok=false
        fi
        # Check JAR file exists
        local jar_path
        jar_path=$(echo "$CLI_CMD" | awk '{print $3}')
        if [[ -n "$jar_path" && ! -f "$jar_path" ]]; then
            error "CLI uber JAR not found: $jar_path"
            error "Build it first: ./gradlew build"
            ok=false
        fi
    else
        if [[ ! -x "$cli_first_word" ]]; then
            error "CLI binary not found or not executable: $cli_first_word"
            ok=false
        fi
    fi

    # Check test JARs exist
    if [[ ! -f "$SPRINGBOOT_JAR" ]]; then
        error "Spring Boot test JAR not found: $SPRINGBOOT_JAR"
        ok=false
    fi
    if [[ ! -f "$STANDALONE_JAR" ]]; then
        error "Standalone test JAR not found: $STANDALONE_JAR"
        ok=false
    fi

    # Check Docker (unless skipping)
    if [[ "$SKIP_DOCKER" != "true" ]]; then
        if ! command -v docker &>/dev/null; then
            error "docker not found in PATH (use --no-docker to skip Docker management)"
            ok=false
        elif ! docker compose version &>/dev/null 2>&1; then
            error "'docker compose' not available (use --no-docker to skip Docker management)"
            ok=false
        fi
    fi

    if [[ "$ok" != "true" ]]; then
        error "Precondition checks failed. Fix the issues above and retry."
        exit 2
    fi
}

# ─── Docker management ────────────────────────────────────────────────────────

docker_up() {
    if [[ "$SKIP_DOCKER" == "true" ]]; then
        info "Skipping Docker (--no-docker)"
        return
    fi

    info "Starting MySQL container (port ${MYSQL_PORT})..."
    MYSQL_PORT="$MYSQL_PORT" \
    MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    MYSQL_USER="$MYSQL_USER" \
    MYSQL_PASSWORD="$MYSQL_PASSWORD" \
        docker compose -f "${SCRIPT_DIR}/docker-compose.yml" up -d --wait

    info "MySQL is ready."
}

docker_down() {
    if [[ "$SKIP_DOCKER" == "true" ]]; then
        return
    fi

    info "Stopping MySQL container..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.yml" down -v --remove-orphans 2>/dev/null || true
}

# ─── Cleanup trap ─────────────────────────────────────────────────────────────

cleanup() {
    local exit_code=$?
    docker_down
    exit "$exit_code"
}

trap cleanup EXIT INT TERM

# ─── Test counters ────────────────────────────────────────────────────────────

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# ─── Core test function ──────────────────────────────────────────────────────

run_test() {
    local name="$1"
    local expected_exit="$2"
    shift 2
    local cli_args=("$@")

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    local actual_exit=0
    local output=""
    local cmd_string="${CLI_CMD} ${cli_args[*]}"

    if [[ "$VERBOSE" == "true" ]]; then
        echo ""
        info "Running: $cmd_string"
    fi

    # Build command with optional timeout
    if [[ -n "$TIMEOUT_CMD" ]]; then
        output=$($TIMEOUT_CMD "${TEST_TIMEOUT}s" $CLI_CMD "${cli_args[@]}" 2>&1) || actual_exit=$?
    else
        output=$($CLI_CMD "${cli_args[@]}" 2>&1) || actual_exit=$?
    fi

    # Handle timeout exit code (124 for GNU timeout)
    if [[ "$actual_exit" -eq 124 && -n "$TIMEOUT_CMD" ]]; then
        fail "${name} (TIMEOUT after ${TEST_TIMEOUT}s)"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        if [[ "$VERBOSE" == "true" ]]; then
            echo "$output"
        fi
        return
    fi

    if [[ "$actual_exit" -eq "$expected_exit" ]]; then
        pass "${name} (exit=${actual_exit})"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        fail "${name} (expected exit=${expected_exit}, got exit=${actual_exit})"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        # Always show output on failure
        echo "$output" | head -40
        if [[ $(echo "$output" | wc -l) -gt 40 ]]; then
            echo "  ... (truncated, use --verbose for full output)"
        fi
    fi

    if [[ "$VERBOSE" == "true" && "$actual_exit" -eq "$expected_exit" ]]; then
        echo "$output"
    fi
}

# ─── JDBC URL builder ─────────────────────────────────────────────────────────

jdbc_url() {
    local db="$1"
    echo "jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}

# ─── Main ─────────────────────────────────────────────────────────────────────

main() {
    echo ""
    echo -e "${BOLD}Flamingock CLI — Integration Tests${RESET}"
    echo "────────────────────────────────────────"
    echo ""
    info "CLI command: ${CLI_CMD}"
    info "MySQL: ${MYSQL_HOST}:${MYSQL_PORT}"
    echo ""

    check_preconditions
    docker_up

    local sb_url
    sb_url=$(jdbc_url "flamingock_test_springboot")
    local sa_url
    sa_url=$(jdbc_url "flamingock_test_standalone")

    # ── Spring Boot JAR tests ─────────────────────────────────────────────

    echo ""
    echo -e "${BOLD}Spring Boot JAR tests${RESET}"
    echo "─────────────────────"

    run_test "springboot: execute apply (first run)" 0 \
        execute apply --jar "$SPRINGBOOT_JAR" \
        -- "--spring.datasource.url=${sb_url}" \
           "--spring.datasource.username=${MYSQL_USER}" \
           "--spring.datasource.password=${MYSQL_PASSWORD}"

    run_test "springboot: audit list" 0 \
        audit list --jar "$SPRINGBOOT_JAR" \
        -- "--spring.datasource.url=${sb_url}" \
           "--spring.datasource.username=${MYSQL_USER}" \
           "--spring.datasource.password=${MYSQL_PASSWORD}"

    run_test "springboot: issue list" 0 \
        issue list --jar "$SPRINGBOOT_JAR" \
        -- "--spring.datasource.url=${sb_url}" \
           "--spring.datasource.username=${MYSQL_USER}" \
           "--spring.datasource.password=${MYSQL_PASSWORD}"

    run_test "springboot: execute apply (idempotent re-run)" 0 \
        execute apply --jar "$SPRINGBOOT_JAR" \
        -- "--spring.datasource.url=${sb_url}" \
           "--spring.datasource.username=${MYSQL_USER}" \
           "--spring.datasource.password=${MYSQL_PASSWORD}"

    # ── Standalone JAR tests ──────────────────────────────────────────────

    echo ""
    echo -e "${BOLD}Standalone JAR tests${RESET}"
    echo "────────────────────"

    run_test "standalone: execute apply (first run)" 0 \
        execute apply --jar "$STANDALONE_JAR" \
        -- "--url=${sa_url}" \
           "--user=${MYSQL_USER}" \
           "--password=${MYSQL_PASSWORD}"

    run_test "standalone: audit list" 0 \
        audit list --jar "$STANDALONE_JAR" \
        -- "--url=${sa_url}" \
           "--user=${MYSQL_USER}" \
           "--password=${MYSQL_PASSWORD}"

    run_test "standalone: issue list" 0 \
        issue list --jar "$STANDALONE_JAR" \
        -- "--url=${sa_url}" \
           "--user=${MYSQL_USER}" \
           "--password=${MYSQL_PASSWORD}"

    run_test "standalone: execute apply (idempotent re-run)" 0 \
        execute apply --jar "$STANDALONE_JAR" \
        -- "--url=${sa_url}" \
           "--user=${MYSQL_USER}" \
           "--password=${MYSQL_PASSWORD}"

    # ── Summary ───────────────────────────────────────────────────────────

    echo ""
    echo "────────────────────────────────────────"
    if [[ "$TESTS_FAILED" -eq 0 ]]; then
        echo -e "${GREEN}${BOLD}ALL TESTS PASSED${RESET} (${TESTS_PASSED}/${TESTS_TOTAL})"
    else
        echo -e "${RED}${BOLD}TESTS FAILED${RESET} (${TESTS_PASSED} passed, ${TESTS_FAILED} failed, ${TESTS_TOTAL} total)"
    fi
    echo ""

    if [[ "$TESTS_FAILED" -gt 0 ]]; then
        exit 1
    fi
}

main
