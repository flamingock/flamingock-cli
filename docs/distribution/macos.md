# macOS Distribution Strategy

> Decision document for Flamingock CLI macOS distribution options.
> Current state: native binary (`flamingock-X.Y.Z-macos-arm64`) published to GitHub Releases with SHA256 checksums.
> Manual install: download, `chmod +x`, `xattr -d com.apple.quarantine`, move to PATH.

---

## Option A: Homebrew Tap (Custom)

### Description

A **Homebrew tap** is a third-party formula repository hosted on GitHub (e.g., `flamingock/homebrew-flamingock`). Users install with `brew tap flamingock/flamingock && brew install flamingock`. The formula downloads the pre-built native binary from GitHub Releases, so no compilation happens on the user's machine.

Homebrew taps **bypass Gatekeeper entirely** because Homebrew itself is already trusted by the system. This eliminates the `xattr -d com.apple.quarantine` workaround completely.

### Setup Steps

1. Create a public GitHub repository: `flamingock/homebrew-flamingock`
2. Add a Ruby formula file `Formula/flamingock.rb`:
   ```ruby
   class Flamingock < Formula
     desc "CLI for audited, synchronized evolution of distributed systems"
     homepage "https://docs.flamingock.io/cli"
     version "1.1.0"
     license "Apache-2.0"

     on_macos do
       if Hardware::CPU.arm?
         url "https://github.com/flamingock/flamingock/releases/download/v#{version}/flamingock-#{version}-macos-arm64"
         sha256 "<SHA256_HASH>"
       end
     end

     on_linux do
       if Hardware::CPU.intel?
         url "https://github.com/flamingock/flamingock/releases/download/v#{version}/flamingock-#{version}-linux-x86_64"
         sha256 "<SHA256_HASH>"
       end
     end

     def install
       binary_name = "flamingock-#{version}-macos-arm64"
       binary_name = "flamingock-#{version}-linux-x86_64" if OS.linux?
       mv binary_name, "flamingock"
       bin.install "flamingock"
     end

     test do
       assert_match "flamingock", shell_output("#{bin}/flamingock --version")
     end
   end
   ```
3. On each release, update the formula with the new version and SHA256 hashes.

### CI/CD Integration

Add a step to the release workflow that:
1. Downloads `SHA256SUMS.txt` from the new release
2. Extracts hashes for macOS and Linux binaries
3. Updates the formula via `sed` or a templating script
4. Commits and pushes to the `homebrew-flamingock` repository

This can be done with a GitHub Actions workflow in the tap repo triggered by `repository_dispatch`, or as a post-release step in the main release workflow using a personal access token (PAT) with write access to the tap repo.

**Tools that automate this**: [GoReleaser](https://goreleaser.com/customization/homebrew/) can auto-generate and push tap formulae. Since Flamingock uses Gradle (not Go), a custom script or GitHub Action is more appropriate.

### Ongoing Maintenance

- **Per release**: Update formula (version + SHA256 hashes). ~5 minutes if automated, ~15 minutes if manual.
- **Periodic**: Address Homebrew formula deprecation warnings if Homebrew API changes (rare, ~1x/year).
- **Testing**: Run `brew audit --strict flamingock` and `brew test flamingock` in CI to catch formula issues early.

### User Experience

```bash
# First-time install
brew tap flamingock/flamingock
brew install flamingock

# Verify
flamingock --version

# Upgrade
brew upgrade flamingock

# Uninstall
brew untap flamingock/flamingock
brew uninstall flamingock
```

No `chmod`, no `xattr`, no manual PATH configuration. Works immediately.

### Cost

**$0** — GitHub hosting is free for public repositories.

### Pros

- Zero cost
- Familiar UX for macOS developers (Homebrew has ~80% adoption among macOS developers)
- Bypasses Gatekeeper — no quarantine attribute issues
- Covers Linux too via Linuxbrew (same formula, same tap)
- Automatic upgrade path (`brew upgrade`)
- No Apple Developer Program needed
- Can be set up in an afternoon

### Cons

- Two-step install (`tap` then `install`) vs one-step for Homebrew Core
- Requires maintaining a separate repository
- Formula updates must be pushed on each release (automatable)
- Users must discover the tap name (documentation/README)
- No discoverability via `brew search` (tap must be added first)

### Comparable Tools

| Tool | Tap | Stars |
|------|-----|-------|
| AWS Copilot | `aws/tap` | ~3.5k |
| GoReleaser | `goreleaser/tap` | ~14k |
| GraalVM | `graalvm/tap` | ~20k |
| Buf | `bufbuild/buf` | ~9k |
| Flyctl | `superfly/tap` | ~1.5k |

### Audience Fit

Developers and DevOps engineers on macOS who use Homebrew daily. This is the **primary audience** for Flamingock CLI on macOS.

---

## Option B: Homebrew Core (Official)

### Description

**Homebrew Core** is the default formula repository. Users install with just `brew install flamingock` — no tap needed. Formulae are submitted as PRs to the [`Homebrew/homebrew-core`](https://github.com/Homebrew/homebrew-core) repository.

### Requirements

Homebrew Core has strict acceptance criteria:
- **Minimum 75 GitHub stars** (hard requirement, checked by reviewers)
- **Must build from source** — Homebrew Core does not accept pre-built binaries (unlike taps)
- **No vendored dependencies** that duplicate what Homebrew already provides
- **Active maintenance** — formulae that fail to build are removed

Building from source means Homebrew CI must:
1. Install GraalVM (via `depends_on "graalvm-jdk"` or a resource block)
2. Run `./gradlew nativeCompile`
3. This takes **10-30 minutes** per architecture and is fragile in Homebrew's sandboxed build environment

### Setup Steps

1. Reach 75+ GitHub stars
2. Write a source-build formula:
   ```ruby
   class Flamingock < Formula
     desc "CLI for audited, synchronized evolution of distributed systems"
     homepage "https://docs.flamingock.io/cli"
     url "https://github.com/flamingock/flamingock/archive/refs/tags/v1.1.0.tar.gz"
     sha256 "<SHA256_HASH>"
     license "Apache-2.0"

     depends_on "graalvm-jdk" => :build
     depends_on "gradle" => :build

     def install
       system "./gradlew", ":cli:flamingock-cli-executor:nativeCompile"
       bin.install "cli/flamingock-cli-executor/build/native/nativeCompile/flamingock"
     end

     test do
       assert_match "flamingock", shell_output("#{bin}/flamingock --version")
     end
   end
   ```
3. Submit PR to `Homebrew/homebrew-core`
4. Address reviewer feedback (typically 1-3 rounds)

### CI/CD Integration

Once accepted, Homebrew's bot ([BrewTestBot](https://github.com/BrewTestBot)) handles building and bottling (pre-built binary caches). Version bumps can be submitted via `brew bump-formula-pr flamingock --url=<new-tarball-url> --sha256=<hash>`.

### Ongoing Maintenance

- **Per release**: Submit a version bump PR (automatable with `brew bump-formula-pr`)
- **Periodic**: Fix build failures if Homebrew updates GraalVM or build environment
- **Risk**: GraalVM native compilation in Homebrew CI is known to be **fragile** — reflection configs, missing libraries, and memory limits cause build failures. Several GraalVM-based tools have had their formulae removed or switched to taps for this reason.

### User Experience

```bash
# Install (one command!)
brew install flamingock

# Upgrade
brew upgrade flamingock

# Uninstall
brew uninstall flamingock
```

### Cost

**$0** — but significant time investment for initial submission and ongoing build maintenance.

### Pros

- One-step install — maximum discoverability
- Discoverable via `brew search flamingock`
- Maintained by Homebrew community (bottles, CI)
- Highest credibility signal for an open-source CLI tool
- Bypasses Gatekeeper (same as tap)

### Cons

- **75+ star requirement** — blocker until project reaches this threshold
- **Must build from source** — GraalVM native compilation in Homebrew CI is fragile
- Slower review cycles (days to weeks for PRs)
- Build failures block releases (Homebrew CI is strict)
- Less control over formula content (reviewers may request changes)
- Risk of formula removal if builds break repeatedly

### Comparable Tools

All major CLI tools are in Homebrew Core: `gh`, `kubectl`, `terraform`, `docker`, `helm`, `jq`, `ripgrep`. Most of these compile from Go or Rust, which are much simpler than GraalVM native image.

### Audience Fit

All macOS Homebrew users. This is a **long-term goal** — pursue after the project has sufficient stars and the GraalVM build is proven stable.

---

## Option C: Apple Code Signing + Notarization

### Description

**Code signing** uses an Apple Developer ID certificate to cryptographically sign the binary. **Notarization** submits the signed binary to Apple's servers for automated malware scanning; if it passes, Apple issues a "ticket" that Gatekeeper trusts.

For **standalone binaries** (not `.app` bundles or `.pkg` installers), the notarization ticket **cannot be stapled** to the binary itself. Instead, Gatekeeper checks Apple's servers online when the binary is first run. This means:
- First run requires an internet connection for Gatekeeper verification
- Subsequent runs are cached locally
- If Apple's servers are unreachable, Gatekeeper may still block the binary (though macOS typically allows after a timeout)

### Setup Steps

1. **Enroll in Apple Developer Program** ($99/year) at [developer.apple.com](https://developer.apple.com)
2. **Create a "Developer ID Application" certificate** in Certificates, Identifiers & Profiles
3. **Export the certificate** as a `.p12` file for CI
4. **Create an app-specific password** for `notarytool` (Apple ID → App-Specific Passwords)
5. **Sign the binary**:
   ```bash
   codesign --force --options runtime \
     --sign "Developer ID Application: Flamingock (TEAM_ID)" \
     --timestamp \
     flamingock
   ```
6. **Notarize** (submit to Apple):
   ```bash
   # Zip the binary (notarytool requires a zip, dmg, or pkg)
   zip flamingock.zip flamingock

   xcrun notarytool submit flamingock.zip \
     --apple-id "dev@flamingock.io" \
     --team-id "TEAM_ID" \
     --password "@keychain:notarytool-password" \
     --wait
   ```
7. **Verify**:
   ```bash
   spctl --assess --type execute flamingock
   # Should output: flamingock: accepted
   ```

### CI/CD Integration

Add a signing + notarization step to the macOS job in `release.yml`:

1. Store the `.p12` certificate and password as GitHub Secrets (`APPLE_CERTIFICATE_BASE64`, `APPLE_CERTIFICATE_PASSWORD`, `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`)
2. Import certificate into a temporary macOS keychain in CI:
   ```yaml
   - name: Import Apple certificate
     run: |
       echo "$APPLE_CERTIFICATE_BASE64" | base64 --decode > certificate.p12
       security create-keychain -p "" build.keychain
       security import certificate.p12 -k build.keychain -P "$APPLE_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
       security set-key-partition-list -S apple-tool:,apple: -s -k "" build.keychain
       security list-keychains -d user -s build.keychain
   ```
3. Sign and notarize after building the native binary
4. **Time cost**: Signing is instant; notarization takes **1-5 minutes** (Apple server-side scan)

### Ongoing Maintenance

- **$99/year** Apple Developer Program renewal
- **Certificate renewal** every 5 years (Developer ID Application certificates are valid for 5 years)
- **CI secrets rotation** when certificate is renewed
- Notarization requirements may change with new macOS versions (Apple tightens rules periodically)

### User Experience

```bash
# Download
curl -LO https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-macos-arm64

# Make executable
chmod +x flamingock-1.1.0-macos-arm64

# Run — NO xattr needed! Gatekeeper trusts the signed+notarized binary
./flamingock-1.1.0-macos-arm64 --version
```

The `xattr -d com.apple.quarantine` step is eliminated. On first run, Gatekeeper silently checks Apple's servers and allows execution.

### Cost

| Item | Cost |
|------|------|
| Apple Developer Program | $99/year |
| CI time (notarization) | 1-5 min per release |

### Pros

- Eliminates `xattr` workaround — professional first-run experience
- Gatekeeper trust — no scary "unidentified developer" warning
- Required foundation for `.pkg` installers (Option D)
- Standard practice for professionally distributed macOS software
- Certificate lasts 5 years between renewals

### Cons

- $99/year ongoing cost
- Standalone binaries cannot be stapled — requires online Gatekeeper check on first run
- Requires macOS CI runner for signing (GitHub Actions provides this)
- Certificate/secret management overhead in CI
- If Apple revokes the certificate (rare), all signed binaries become untrusted
- Doesn't help with PATH setup or upgrades (still manual)

### Comparable Tools

Almost all professionally distributed macOS CLI tools are signed and notarized: Docker Desktop, 1Password CLI, Tailscale, Datadog Agent. Many smaller tools skip this and rely on Homebrew instead.

### Audience Fit

Users who download binaries directly (not via Homebrew). Important for enterprise environments with strict Gatekeeper policies.

---

## Option D: .pkg Installer

### Description

A `.pkg` installer is a macOS installer package that provides a double-click GUI installation experience. Unlike standalone binaries, `.pkg` files **can be stapled** with the notarization ticket, meaning Gatekeeper verification works fully offline.

The installer can:
- Copy the binary to `/usr/local/bin` (or another location)
- Set up PATH entries
- Show license agreements
- Run pre/post-install scripts

### Setup Steps

1. **Prerequisites**: Apple Developer Program ($99/year) with **two certificates**:
   - "Developer ID Application" — for signing the binary inside the package
   - "Developer ID Installer" — for signing the `.pkg` itself
2. **Build the .pkg**:
   ```bash
   # Sign the binary first
   codesign --force --options runtime \
     --sign "Developer ID Application: Flamingock (TEAM_ID)" \
     flamingock

   # Create the pkg
   pkgbuild --root ./payload \
     --identifier io.flamingock.cli \
     --version 1.1.0 \
     --install-location /usr/local/bin \
     --sign "Developer ID Installer: Flamingock (TEAM_ID)" \
     flamingock-1.1.0.pkg
   ```
3. **Notarize the .pkg**:
   ```bash
   xcrun notarytool submit flamingock-1.1.0.pkg \
     --apple-id "dev@flamingock.io" \
     --team-id "TEAM_ID" \
     --password "@keychain:notarytool-password" \
     --wait
   ```
4. **Staple the ticket** (makes offline verification work):
   ```bash
   xcrun stapler staple flamingock-1.1.0.pkg
   ```

### CI/CD Integration

Extension of Option C's CI setup:
1. Same certificate import steps, plus the "Developer ID Installer" certificate
2. After native image build: sign binary → build `.pkg` → sign `.pkg` → notarize → staple
3. Upload `.pkg` as an additional release artifact
4. **Time cost**: Adds ~2-5 minutes to the release pipeline

### Ongoing Maintenance

- Same as Option C ($99/year, certificate renewal)
- `.pkg` build scripts must be updated if install paths or components change
- Test on each major macOS release for compatibility

### User Experience

```bash
# Download
curl -LO https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0.pkg

# Install (GUI double-click, or command line)
sudo installer -pkg flamingock-1.1.0.pkg -target /

# Verify
flamingock --version

# Uninstall (manual — pkgs don't have built-in uninstall)
sudo rm /usr/local/bin/flamingock
```

### Cost

| Item | Cost |
|------|------|
| Apple Developer Program | $99/year (same as Option C) |
| Two certificates needed | Included in program |
| CI time | +2-5 min per release |

### Pros

- Stapled notarization — works fully offline
- Professional GUI install experience
- Can be distributed via MDM (Jamf, Mosyle, etc.) for enterprise
- Double-click install — accessible to non-technical users
- No `chmod`, no `xattr`, no PATH configuration

### Cons

- **Overkill for CLI users** — CLI developers prefer `brew install` or `curl | bash`
- Requires `sudo` for system-wide install
- No built-in uninstall mechanism (macOS `.pkg` limitation)
- No built-in upgrade mechanism (must download new `.pkg`)
- Two certificates to manage instead of one
- More complex CI pipeline
- Enterprise MDM distribution is rarely needed for developer tools

### Comparable Tools

| Tool | Uses .pkg? |
|------|-----------|
| AWS CLI v2 | Yes (primary macOS distribution) |
| Docker Desktop | Yes (via `.dmg` containing `.app`) |
| Xcode CLI Tools | Yes |
| Most CLI tools | No — prefer Homebrew or direct binary |

### Audience Fit

Enterprise environments with MDM deployment. **Not recommended** as a priority for Flamingock CLI given the developer-focused audience.

---

## Option E: MacPorts

### Description

[MacPorts](https://www.macports.org/) is an alternative macOS package manager, older than Homebrew. It installs packages into `/opt/local/` and uses its own dependency tree. Ports are defined in `Portfiles` and submitted to the [MacPorts repository](https://github.com/macports/macports-ports).

### Setup Steps

1. Write a `Portfile`:
   ```tcl
   PortSystem          1.0
   name                flamingock
   version             1.1.0
   categories          devel java
   license             Apache-2
   maintainers         {flamingock.io:dev}
   description         CLI for audited, synchronized evolution of distributed systems
   long_description    ${description}
   homepage            https://docs.flamingock.io/cli
   master_sites        https://github.com/flamingock/flamingock/releases/download/v${version}/
   distname            flamingock-${version}-macos-arm64
   checksums           sha256 <HASH>
   extract.suffix
   use_configure       no
   build               {}
   destroot {
       xinstall -m 755 ${distpath}/${distname} ${destroot}${prefix}/bin/flamingock
   }
   ```
2. Submit PR to `macports/macports-ports`
3. Address reviewer feedback

### CI/CD Integration

Version bump PRs to `macports/macports-ports` on each release. Can be automated with a script, but MacPorts has no equivalent of `brew bump-formula-pr`.

### Ongoing Maintenance

- **Per release**: Submit version bump PR with updated checksums
- MacPorts review process is slower than Homebrew (smaller maintainer pool)
- Must handle architecture variants (ARM64 vs x86_64) in the Portfile

### User Experience

```bash
# Install
sudo port install flamingock

# Upgrade
sudo port upgrade flamingock

# Uninstall
sudo port uninstall flamingock
```

### Cost

**$0**

### Pros

- Covers a different (small) segment of macOS users
- Well-established package manager with 20+ year history
- Strict isolation from system libraries (`/opt/local/`)

### Cons

- **Very small user base** compared to Homebrew (<5% of macOS developers)
- Requires `sudo` for all operations
- Slower review process
- Build-from-source default (pre-built binaries are possible but less common)
- GraalVM native compilation in MacPorts build environment is untested and likely problematic
- ROI is very low given the small audience

### Comparable Tools

Some tools like `cmake`, `python`, and `git` are available via MacPorts, but most modern CLI tools prioritize Homebrew exclusively.

### Audience Fit

Legacy macOS users who prefer MacPorts over Homebrew. **Very low priority** for Flamingock CLI.

---

## Option F: Direct Download Improvements (Install Script)

### Description

An **install script** provides a `curl | bash` one-liner that automates the full install process: platform detection, download, SHA256 verification, placement on PATH, and post-install verification. This is the fastest path to a good install experience without any package manager dependency.

### Setup Steps

1. **Write `install.sh`** (hosted in the repository or on a CDN):
   ```bash
   #!/usr/bin/env bash
   set -euo pipefail

   # Wrap in function to ensure complete download before execution
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

     # Resolve latest version if needed
     if [ "$VERSION" = "latest" ]; then
       VERSION=$(curl -fsSL "https://api.github.com/repos/flamingock/flamingock/releases/latest" \
         | grep '"tag_name"' | sed -E 's/.*"v([^"]+)".*/\1/')
     fi

     local BINARY="flamingock-${VERSION}-${OS}-${ARCH}"
     local URL="https://github.com/flamingock/flamingock/releases/download/v${VERSION}/${BINARY}"
     local CHECKSUMS_URL="https://github.com/flamingock/flamingock/releases/download/v${VERSION}/SHA256SUMS.txt"

     echo "Installing Flamingock CLI v${VERSION} (${OS}/${ARCH})..."

     # Download binary and checksums
     local TMP_DIR
     TMP_DIR="$(mktemp -d)"
     trap 'rm -rf "$TMP_DIR"' EXIT

     curl -fsSL -o "${TMP_DIR}/flamingock" "$URL"
     curl -fsSL -o "${TMP_DIR}/SHA256SUMS.txt" "$CHECKSUMS_URL"

     # Verify SHA256
     local EXPECTED_HASH ACTUAL_HASH
     EXPECTED_HASH=$(grep "$BINARY" "${TMP_DIR}/SHA256SUMS.txt" | awk '{print $1}')
     ACTUAL_HASH=$(shasum -a 256 "${TMP_DIR}/flamingock" | awk '{print $1}')

     if [ "$EXPECTED_HASH" != "$ACTUAL_HASH" ]; then
       echo "Error: SHA256 checksum mismatch!"
       echo "  Expected: $EXPECTED_HASH"
       echo "  Actual:   $ACTUAL_HASH"
       exit 1
     fi

     # Install
     chmod +x "${TMP_DIR}/flamingock"
     if [ -w "$INSTALL_DIR" ]; then
       mv "${TMP_DIR}/flamingock" "${INSTALL_DIR}/flamingock"
     else
       sudo mv "${TMP_DIR}/flamingock" "${INSTALL_DIR}/flamingock"
     fi

     echo "Flamingock CLI v${VERSION} installed to ${INSTALL_DIR}/flamingock"
     flamingock --version
   }

   install_flamingock
   ```
2. **Host the script**: Commit to the repository (e.g., `scripts/install.sh`) and serve via GitHub raw URL. Optionally set up a vanity URL: `https://get.flamingock.io` (CNAME to GitHub Pages or CDN).
3. **Document on README and website**.

### CI/CD Integration

The install script itself doesn't require CI changes. It reads from existing GitHub Release artifacts. Test the script in CI (e.g., run it in a matrix of OS containers) to ensure it works across platforms.

### Ongoing Maintenance

- **Minimal** — the script auto-resolves the latest version from GitHub Releases
- Update script if artifact naming conventions change
- Test periodically on new OS versions

### User Experience

```bash
# One-liner install
curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# With specific version
FLAMINGOCK_VERSION=1.1.0 curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# With custom install directory
FLAMINGOCK_INSTALL_DIR=~/.local/bin curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# With vanity URL (if set up)
curl -fsSL https://get.flamingock.io | bash
```

### Cost

**$0** (or ~$10/year for a vanity domain if desired)

### Pros

- Zero dependencies — works on any macOS or Linux system with `curl` and `bash`
- Automated platform detection, SHA256 verification, and PATH setup
- Familiar pattern — widely used and accepted by developers
- Works in CI/CD environments (Docker, GitHub Actions, etc.)
- No package manager required
- Covers macOS AND Linux with one script
- Can be set up in a few hours

### Cons

- `curl | bash` has security perception issues (mitigated by SHA256 verification and HTTPS)
- No automatic upgrade mechanism
- No uninstall command (manual `rm`)
- Requires `sudo` if installing to `/usr/local/bin` on some systems
- Gatekeeper still applies if not installing via Homebrew (unless code-signed per Option C)
- Users must trust the script source

### Comparable Tools

| Tool | Install command |
|------|----------------|
| Rust/rustup | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` |
| Deno | `curl -fsSL https://deno.land/install.sh \| sh` |
| Fly.io | `curl -L https://fly.io/install.sh \| sh` |
| Helm | `curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \| bash` |
| Homebrew | `curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh \| bash` |

### Audience Fit

Developers in CI/CD pipelines, Docker containers, and anyone who prefers a quick one-liner over package manager setup. Good complement to Homebrew.

---

## Comparison Matrix

| Criterion | A. Homebrew Tap | B. Homebrew Core | C. Code Signing | D. .pkg | E. MacPorts | F. Install Script |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|
| **Setup effort** | Low | High | Medium | High | Medium | Low |
| **Cost** | $0 | $0 | $99/yr | $99/yr | $0 | $0 |
| **Gatekeeper bypass** | Yes | Yes | Yes | Yes (offline) | Partial | No |
| **Auto-upgrade** | Yes | Yes | No | No | Yes | No |
| **Discoverability** | Low | High | N/A | N/A | Low | Low |
| **CI/CD friendly** | Yes | N/A | N/A | N/A | No | Yes |
| **Maintenance** | Low | High | Low | Medium | Medium | Minimal |
| **Audience reach** | High | Highest | Medium | Low | Very low | High |
| **Star requirement** | No | 75+ | No | No | No | No |
| **Covers Linux too** | Yes | No | No | No | No | Yes |

---

## Suggested Progression Path

### Phase 1 — Immediate (Week 1-2)

**F. Install Script + A. Homebrew Tap**

These two options together cover the vast majority of macOS users at zero cost:
- Install script provides the universal `curl | bash` experience and also works for Linux
- Homebrew tap provides the native macOS package manager experience
- Both bypass the `xattr` quarantine issue (Homebrew inherently, script by downloading to a temp dir)

### Phase 2 — When Justified by Adoption (~6 months)

**C. Apple Code Signing + Notarization**

Once the project has enough users that the $99/year cost is justified:
- Eliminates Gatekeeper warnings for direct binary downloads
- Professional credibility signal
- Required foundation if `.pkg` is ever needed

### Phase 3 — Long-term Goal (When Stars > 75)

**B. Homebrew Core**

Submit to Homebrew Core when:
- Project has 75+ GitHub stars
- GraalVM native compilation is proven stable in CI
- There's demand from users who don't want to add a tap

### Deprioritized

- **D. .pkg Installer** — Only pursue if there's specific enterprise demand for MDM distribution
- **E. MacPorts** — Only pursue if users explicitly request it; ROI is too low otherwise
