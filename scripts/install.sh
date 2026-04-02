#!/usr/bin/env bash
set -euo pipefail

install_flamingock() {
  local VERSION="${FLAMINGOCK_VERSION:-latest}"
  local INSTALL_DIR="${FLAMINGOCK_INSTALL_DIR:-/usr/local/bin}"

  # Detect platform
  local OS ARCH
  OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
  ARCH="$(uname -m)"

  case "$OS" in
    darwin) OS="macos" ;;
    linux)  OS="linux" ;;
    *)      echo "Error: Unsupported OS: $OS"; exit 1 ;;
  esac

  case "$ARCH" in
    arm64|aarch64) ARCH="arm64" ;;
    x86_64|amd64)  ARCH="x86_64" ;;
    *)             echo "Error: Unsupported architecture: $ARCH"; exit 1 ;;
  esac

  # Linux ARM64 has no native binary — redirect to the JVM JAR
  if [ "$OS" = "linux" ] && [ "$ARCH" = "arm64" ]; then
    echo "Error: No native binary available for Linux ARM64."
    echo ""
    echo "Use the platform-independent JAR instead (requires JRE 21+):"
    echo "  https://github.com/flamingock/flamingock-cli/releases/latest"
    exit 1
  fi

  # Resolve latest version
  if [ "$VERSION" = "latest" ]; then
    VERSION=$(curl -fsSL "https://api.github.com/repos/flamingock/flamingock-cli/releases/latest" \
      | grep '"tag_name"' | sed -E 's/.*"v([^"]+)".*/\1/')
  fi

  local BINARY="flamingock-${VERSION}-${OS}-${ARCH}"
  local URL="https://github.com/flamingock/flamingock-cli/releases/download/v${VERSION}/${BINARY}"
  local CHECKSUMS_URL="https://github.com/flamingock/flamingock-cli/releases/download/v${VERSION}/SHA256SUMS.txt"

  echo "Installing Flamingock CLI v${VERSION} (${OS}/${ARCH})..."

  # Download to temp directory
  curl -fsSL -o "${TMP_DIR}/flamingock" "$URL"
  curl -fsSL -o "${TMP_DIR}/SHA256SUMS.txt" "$CHECKSUMS_URL"

  # Verify SHA256
  local EXPECTED_HASH ACTUAL_HASH
  EXPECTED_HASH=$(grep "$BINARY" "${TMP_DIR}/SHA256SUMS.txt" | awk '{print $1}')
  if command -v sha256sum &>/dev/null; then
    ACTUAL_HASH=$(sha256sum "${TMP_DIR}/flamingock" | awk '{print $1}')
  else
    ACTUAL_HASH=$(shasum -a 256 "${TMP_DIR}/flamingock" | awk '{print $1}')
  fi

  if [ "$EXPECTED_HASH" != "$ACTUAL_HASH" ]; then
    echo "Error: SHA256 checksum mismatch!"
    echo "  Expected: $EXPECTED_HASH"
    echo "  Actual:   $ACTUAL_HASH"
    exit 1
  fi

  echo "SHA256 checksum verified."

  # Remove macOS quarantine attribute (required for unsigned binaries downloaded via curl)
  if [ "$OS" = "macos" ]; then
    xattr -d com.apple.quarantine "${TMP_DIR}/flamingock" 2>/dev/null || true
  fi

  # Install
  chmod +x "${TMP_DIR}/flamingock"
  if [ -w "$INSTALL_DIR" ]; then
    mv "${TMP_DIR}/flamingock" "${INSTALL_DIR}/flamingock"
  else
    echo "Installing to ${INSTALL_DIR} (requires sudo)..."
    sudo mv "${TMP_DIR}/flamingock" "${INSTALL_DIR}/flamingock"
  fi

  echo "Flamingock CLI v${VERSION} installed to ${INSTALL_DIR}/flamingock"
  "${INSTALL_DIR}/flamingock" --version
}

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

install_flamingock
