# Linux Distribution Strategy

> Decision document for Flamingock CLI Linux distribution options.
> Current state: native binary (`flamingock-X.Y.Z-linux-x86_64`) published to GitHub Releases with SHA256 checksums.
> Manual install: download, `chmod +x`, move to PATH.

---

## Option A: APT Repository (.deb packages)

### Description

An **APT repository** distributes `.deb` packages for Debian-based distributions (Debian, Ubuntu, Linux Mint, Pop!_OS). Users add the repository to their sources list and install with `apt install flamingock`. Packages are GPG-signed for authenticity.

Hosting options:
- **GitHub Pages** — free, simple, limited to small repos
- **Amazon S3 / Cloudflare R2** — cheap, scalable, requires setup
- **packagecloud.io** — managed service, free tier (1 repo, 500 MB), $15+/month for more
- **Official distro repos** — maximum reach but very slow acceptance process (months to years)

For Flamingock, self-hosted on GitHub Pages or a cloud storage bucket is the most practical starting point.

### Setup Steps

1. **Create the `.deb` package** using `fpm` (effing package management):
   ```bash
   fpm -s dir -t deb \
     --name flamingock \
     --version 1.1.0 \
     --architecture amd64 \
     --description "CLI for audited, synchronized evolution of distributed systems" \
     --url "https://docs.flamingock.io/cli" \
     --license "Apache-2.0" \
     --maintainer "Flamingock <dev@flamingock.io>" \
     ./flamingock-1.1.0-linux-x86_64=/usr/local/bin/flamingock
   ```
2. **Generate a GPG key** for signing:
   ```bash
   gpg --batch --gen-key <<EOF
   Key-Type: RSA
   Key-Length: 4096
   Name-Real: Flamingock
   Name-Email: dev@flamingock.io
   Expire-Date: 0
   %no-protection
   EOF
   ```
3. **Create the APT repository structure**:
   ```bash
   mkdir -p repo/pool/main repo/dists/stable/main/binary-amd64
   cp flamingock_1.1.0_amd64.deb repo/pool/main/
   cd repo
   dpkg-scanpackages pool/ > dists/stable/main/binary-amd64/Packages
   gzip -k dists/stable/main/binary-amd64/Packages
   # Generate Release file and sign it
   apt-ftparchive release dists/stable > dists/stable/Release
   gpg --armor --detach-sign -o dists/stable/Release.gpg dists/stable/Release
   gpg --armor --clearsign -o dists/stable/InRelease dists/stable/Release
   ```
4. **Host the repository** (e.g., GitHub Pages, S3, R2)
5. **Export and publish the GPG public key**

Alternatively, use **[reprepro](https://wiki.debian.org/DebianRepository/SetupWithReprepro)** or **[aptly](https://www.aptly.info/)** for a more robust repository management workflow.

### CI/CD Integration

Add a job to the release workflow:
1. Download the Linux native binary from the build job
2. Build the `.deb` package with `fpm`
3. Sign the package with the GPG key (stored as a GitHub Secret)
4. Update the APT repository (add new package, regenerate indices)
5. Push to hosting (GitHub Pages deploy, S3 sync, etc.)

### Ongoing Maintenance

- **Per release**: Build `.deb`, update repo index, push. ~10 minutes if automated.
- **GPG key management**: Rotate keys before expiry, publish new key, handle transition period
- **Repository hosting**: Monitor storage costs if using cloud storage (minimal for binary packages)
- **Distro compatibility**: Test on current LTS releases (Ubuntu 22.04, 24.04; Debian 12)

### User Experience

```bash
# Add GPG key
curl -fsSL https://flamingock.io/gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/flamingock.gpg

# Add repository
echo "deb [signed-by=/usr/share/keyrings/flamingock.gpg] https://apt.flamingock.io stable main" \
  | sudo tee /etc/apt/sources.list.d/flamingock.list

# Install
sudo apt update && sudo apt install flamingock

# Upgrade (comes with regular system updates)
sudo apt update && sudo apt upgrade flamingock

# Uninstall
sudo apt remove flamingock
```

### Cost

| Hosting | Cost |
|---------|------|
| GitHub Pages | $0 |
| S3/R2 | ~$1-5/month |
| packagecloud.io | $0 (free tier) to $15+/month |

### Pros

- Native package management — installs, upgrades, and removes cleanly
- Automatic upgrades via `apt upgrade`
- Familiar workflow for Debian/Ubuntu users and administrators
- GPG signing provides supply chain security
- Works in Docker containers (`apt install` in Dockerfile)
- Can add as a step in provisioning scripts (Ansible, Chef, Puppet)

### Cons

- Only covers Debian-based distributions
- GPG key management is an ongoing responsibility
- Repository hosting and maintenance overhead
- Initial setup is moderately complex
- Requires `sudo` for install
- Multi-architecture support (amd64 + arm64) doubles the packaging work

### Comparable Tools

| Tool | APT repo URL |
|------|-------------|
| Docker | `https://download.docker.com/linux/ubuntu` |
| GitHub CLI | `https://cli.github.com/packages` |
| HashiCorp | `https://apt.releases.hashicorp.com` |
| Kubernetes | `https://pkgs.k8s.io` |
| Grafana | `https://apt.grafana.com` |

### Audience Fit

DevOps teams, platform engineers, and administrators running Debian/Ubuntu servers. Essential for **production server deployments** where package managers are the standard for software management.

---

## Option B: YUM/DNF Repository (.rpm packages)

### Description

A **YUM/DNF repository** distributes `.rpm` packages for Red Hat-based distributions (RHEL, CentOS, Rocky Linux, AlmaLinux, Fedora, Amazon Linux). Users add the repository config and install with `yum install flamingock` or `dnf install flamingock`.

This is the counterpart to APT for the Red Hat ecosystem, which is dominant in **enterprise server environments** and AWS (Amazon Linux).

### Setup Steps

1. **Create the `.rpm` package** using `fpm`:
   ```bash
   fpm -s dir -t rpm \
     --name flamingock \
     --version 1.1.0 \
     --architecture x86_64 \
     --description "CLI for audited, synchronized evolution of distributed systems" \
     --url "https://docs.flamingock.io/cli" \
     --license "Apache-2.0" \
     --maintainer "Flamingock <dev@flamingock.io>" \
     ./flamingock-1.1.0-linux-x86_64=/usr/local/bin/flamingock
   ```
2. **Create the repository structure**:
   ```bash
   mkdir -p repo/packages
   cp flamingock-1.1.0-1.x86_64.rpm repo/packages/
   createrepo_c repo/
   ```
3. **Sign the RPM and repo metadata** with GPG:
   ```bash
   rpm --addsign repo/packages/flamingock-1.1.0-1.x86_64.rpm
   gpg --armor --detach-sign repo/repodata/repomd.xml
   ```
4. **Host the repository** (same options as APT: GitHub Pages, S3, R2, packagecloud.io)
5. **Create a `.repo` file** for users:
   ```ini
   [flamingock]
   name=Flamingock CLI
   baseurl=https://rpm.flamingock.io/
   enabled=1
   gpgcheck=1
   gpgkey=https://flamingock.io/gpg.key
   ```

### CI/CD Integration

Same pattern as APT:
1. Build `.rpm` with `fpm`
2. Sign with GPG
3. Update repository with `createrepo_c`
4. Push to hosting

Can share the same GPG key as the APT repository.

### Ongoing Maintenance

- **Per release**: Build `.rpm`, update repo, push. Same effort as APT.
- Same GPG key management considerations
- Test on RHEL/CentOS LTS versions and Amazon Linux

### User Experience

```bash
# Add repository
sudo tee /etc/yum.repos.d/flamingock.repo <<EOF
[flamingock]
name=Flamingock CLI
baseurl=https://rpm.flamingock.io/
enabled=1
gpgcheck=1
gpgkey=https://flamingock.io/gpg.key
EOF

# Install
sudo yum install flamingock    # or: sudo dnf install flamingock

# Upgrade
sudo yum update flamingock

# Uninstall
sudo yum remove flamingock
```

### Cost

Same as APT — $0 to $15+/month depending on hosting choice.

### Pros

- Covers RHEL, CentOS, Rocky, AlmaLinux, Fedora, Amazon Linux — dominant in enterprise
- Native package management with automatic upgrades
- RPM signing provides supply chain security
- Essential for AWS environments (Amazon Linux is RPM-based)
- Works in Docker containers and provisioning tools

### Cons

- Only covers RPM-based distributions
- Combined with APT, doubles the packaging pipeline
- `createrepo_c` requires specific tooling in CI
- Requires `sudo`
- GPG key management overhead

### Comparable Tools

| Tool | RPM repo |
|------|----------|
| Docker | `https://download.docker.com/linux/centos` |
| GitHub CLI | `https://cli.github.com/packages` |
| HashiCorp | `https://rpm.releases.hashicorp.com` |
| Grafana | `https://rpm.grafana.com` |

### Audience Fit

Enterprise environments running RHEL, CentOS, or Amazon Linux. **High priority** if targeting AWS-based deployments.

---

## Option C: Snap

### Description

[Snap](https://snapcraft.io/) is Canonical's universal Linux packaging format. Snaps are containerized applications that bundle all dependencies and run in a sandboxed environment. Distributed through the Snap Store.

**Key issue for Flamingock**: The CLI spawns external JVM processes on arbitrary user-specified paths. This requires **classic confinement** (full system access), which:
- Defeats the sandboxing benefits of Snap
- Requires **manual approval** from the Snap team (process can take weeks)
- Is only granted for established tools with a strong justification

### Setup Steps

1. **Write `snapcraft.yaml`**:
   ```yaml
   name: flamingock
   base: core22
   version: '1.1.0'
   summary: CLI for audited, synchronized evolution of distributed systems
   description: |
     Flamingock CLI enables executing Flamingock operations outside
     the normal application lifecycle.
   confinement: classic  # Required: spawns JVM processes
   grade: stable
   architectures:
     - build-on: amd64

   apps:
     flamingock:
       command: bin/flamingock

   parts:
     flamingock:
       plugin: dump
       source: https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-linux-x86_64
       source-type: file
       organize:
         flamingock-1.1.0-linux-x86_64: bin/flamingock
   ```
2. **Build**: `snapcraft`
3. **Request classic confinement**: File a request in the [Snapcraft forum](https://forum.snapcraft.io/c/store-requests/) with justification
4. **Publish**: `snapcraft upload flamingock_1.1.0_amd64.snap --release=stable`

### CI/CD Integration

Use the official [`snapcore/action-publish`](https://github.com/snapcore/action-publish) GitHub Action:
1. Store Snap Store credentials as GitHub Secret
2. Build snap in CI
3. Upload and release to the Snap Store

### Ongoing Maintenance

- **Per release**: Rebuild and upload snap. Automatable.
- Classic confinement approval is one-time but may require re-justification if Snap policies change
- Monitor Snap build base updates (`core22` → `core24`, etc.)

### User Experience

```bash
# Install
sudo snap install flamingock --classic

# Upgrade (automatic by default, or manual)
sudo snap refresh flamingock

# Uninstall
sudo snap remove flamingock
```

### Cost

**$0** — Snap Store is free for open-source projects.

### Pros

- Wide reach — pre-installed on Ubuntu, available on most Linux distros
- Automatic updates (background refresh)
- Single package format for all distributions
- Snap Store provides a web presence and discoverability

### Cons

- **Classic confinement required** — manual approval process, may be denied
- **Controversial in the Linux community** — Snap is divisive (auto-updates, daemon, Canonical control)
- Startup overhead (snap mount + confinement setup adds ~100-500ms)
- Not available on minimal servers/containers (no `snapd`)
- Linux Mint and some distros actively block Snap
- Competes with Flatpak (which is desktop-focused, not relevant for CLIs)

### Comparable Tools

Few CLI tools use Snap with classic confinement. Most Linux CLI tools prefer APT/YUM or direct download.

| Tool | Snap? |
|------|-------|
| kubectl | Yes (classic) |
| doctl | Yes (classic) |
| Most CLI tools | No |

### Audience Fit

Ubuntu desktop users. **Low priority** due to classic confinement requirement and community controversy.

---

## Option D: Homebrew / Linuxbrew

### Description

[Linuxbrew](https://docs.brew.sh/Homebrew-on-Linux) is Homebrew for Linux. A single Homebrew tap formula can serve both macOS and Linux users. If a Homebrew tap is already set up for macOS (see macOS Option A), extending it to Linux requires only adding the Linux binary URL and hash to the same formula.

### Setup Steps

If the macOS Homebrew tap already exists, extend the formula:
```ruby
class Flamingock < Formula
  desc "CLI for audited, synchronized evolution of distributed systems"
  homepage "https://docs.flamingock.io/cli"
  version "1.1.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/flamingock/flamingock/releases/download/v#{version}/flamingock-#{version}-macos-arm64"
      sha256 "<MACOS_SHA256>"
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "https://github.com/flamingock/flamingock/releases/download/v#{version}/flamingock-#{version}-linux-x86_64"
      sha256 "<LINUX_SHA256>"
    end
  end

  def install
    # Binary name varies by platform
    bin.install Dir["flamingock-*"].first => "flamingock"
  end

  test do
    assert_match "flamingock", shell_output("#{bin}/flamingock --version")
  end
end
```

No additional setup if the tap already exists.

### CI/CD Integration

Same as macOS Homebrew tap — update formula on each release. One formula update covers both platforms.

### Ongoing Maintenance

- **Effectively zero incremental maintenance** over macOS tap
- Same formula serves both platforms

### User Experience

```bash
# Install (same commands as macOS)
brew tap flamingock/flamingock
brew install flamingock

# Upgrade
brew upgrade flamingock

# Uninstall
brew uninstall flamingock
```

### Cost

**$0** (shared with macOS tap)

### Pros

- Zero incremental effort if macOS tap exists
- Single formula for macOS + Linux
- No `sudo` required
- Familiar UX for developers who use Homebrew on Linux
- Installs to `~/.linuxbrew/` — no system-wide changes

### Cons

- **Not available on minimal servers/containers** (Homebrew is heavy: ~1 GB install)
- Not the standard Linux package manager — most admins use APT/YUM
- Requires Ruby and build tools
- Slower install than APT/YUM (Homebrew itself must be installed first)
- Not appropriate for production server deployments

### Comparable Tools

| Tool | Linuxbrew? |
|------|-----------|
| Terraform | Yes (`hashicorp/tap`) |
| Pulumi | Yes |
| k9s | Yes |
| Flyctl | Yes (`superfly/tap`) |

### Audience Fit

Developers on Linux workstations who already use Homebrew. Good complement to APT/YUM but **not a replacement** for server environments.

---

## Option E: AUR (Arch User Repository)

### Description

The [AUR](https://aur.archlinux.org/) is a community-driven repository for Arch Linux. Packages are defined by `PKGBUILD` scripts. For pre-built binaries, the convention is to use a `-bin` suffix (e.g., `flamingock-bin`).

The AUR is trivially easy to set up and maintain, and Arch users are vocal advocates who generate community goodwill disproportionate to their numbers.

### Setup Steps

1. **Create an AUR account** at [aur.archlinux.org](https://aur.archlinux.org)
2. **Write `PKGBUILD`**:
   ```bash
   # Maintainer: Flamingock <dev@flamingock.io>
   pkgname=flamingock-bin
   pkgver=1.1.0
   pkgrel=1
   pkgdesc="CLI for audited, synchronized evolution of distributed systems"
   arch=('x86_64')
   url="https://docs.flamingock.io/cli"
   license=('Apache-2.0')
   provides=('flamingock')
   conflicts=('flamingock')
   source=("https://github.com/flamingock/flamingock/releases/download/v${pkgver}/flamingock-${pkgver}-linux-x86_64")
   sha256sums=('<SHA256_HASH>')

   package() {
     install -Dm755 "flamingock-${pkgver}-linux-x86_64" "${pkgdir}/usr/bin/flamingock"
   }
   ```
3. **Write `.SRCINFO`**: `makepkg --printsrcinfo > .SRCINFO`
4. **Push to AUR**:
   ```bash
   git clone ssh://aur@aur.archlinux.org/flamingock-bin.git
   cp PKGBUILD .SRCINFO flamingock-bin/
   cd flamingock-bin && git add -A && git commit -m "Initial upload: 1.1.0" && git push
   ```

### CI/CD Integration

On each release, update `PKGBUILD` (version + checksum) and push to AUR. Can be automated with a GitHub Action:
```yaml
- uses: KSXGitHub/github-actions-deploy-aur@v3
  with:
    pkgname: flamingock-bin
    pkgbuild: ./aur/PKGBUILD
    commit_username: flamingock-bot
    commit_email: dev@flamingock.io
    ssh_private_key: ${{ secrets.AUR_SSH_KEY }}
```

### Ongoing Maintenance

- **Per release**: Update version and hash in PKGBUILD. ~5 minutes.
- **Very low maintenance** — AUR packages are simple for binary distributions
- Occasional community comments on the AUR page (respond promptly for goodwill)

### User Experience

```bash
# Install with AUR helper (e.g., yay, paru)
yay -S flamingock-bin

# Or manually
git clone https://aur.archlinux.org/flamingock-bin.git
cd flamingock-bin && makepkg -si

# Upgrade
yay -Syu flamingock-bin

# Uninstall
sudo pacman -R flamingock-bin
```

### Cost

**$0**

### Pros

- Trivially easy to set up and maintain
- Arch users are enthusiastic early adopters and vocal advocates
- Community goodwill disproportionate to user numbers
- Good entry in "How to install" documentation
- `-bin` packages are accepted for pre-built binaries (no build-from-source issues)

### Cons

- **Very small audience** — Arch Linux has ~2-3% of Linux desktop share
- Requires users to have an AUR helper (yay, paru) or build manually
- Not suitable for server deployments
- AUR packages are not officially supported by Arch (user responsibility)

### Comparable Tools

| Tool | AUR package |
|------|------------|
| kubectl | `kubectl-bin` |
| terraform | `terraform-bin` |
| gh | `github-cli-bin` |
| ripgrep | `ripgrep-bin` |

### Audience Fit

Arch Linux users (developers, enthusiasts). **Low effort, low reach, high goodwill**. Good for community building.

---

## Option F: Nix / nixpkgs

### Description

[Nix](https://nixos.org/) is a purely functional package manager growing rapidly in the DevOps community. Packages are defined in the Nix expression language. Nix provides two distribution paths:
- **nixpkgs** — the official package repository (like Homebrew Core)
- **Nix flakes** — self-hosted package definitions (like a Homebrew tap)

GraalVM native binaries on NixOS require `autoPatchelfHook` because NixOS doesn't use the standard FHS (Filesystem Hierarchy Standard). Binaries that link against `/lib/ld-linux-x86-64.so.2` will fail without patching.

### Setup Steps

**Option 1: Nix Flake (self-hosted)**

Add a `flake.nix` to the Flamingock repository:
```nix
{
  description = "Flamingock CLI";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs = { self, nixpkgs }:
    let
      supportedSystems = [ "x86_64-linux" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in {
      packages = forAllSystems (system:
        let pkgs = nixpkgs.legacyPackages.${system};
        in {
          default = pkgs.stdenv.mkDerivation {
            pname = "flamingock";
            version = "1.1.0";

            src = pkgs.fetchurl {
              url = "https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-${
                if system == "x86_64-linux" then "linux-x86_64"
                else "macos-arm64"
              }";
              sha256 = if system == "x86_64-linux"
                then "<LINUX_SHA256>"
                else "<MACOS_SHA256>";
            };

            nativeBuildInputs = pkgs.lib.optionals pkgs.stdenv.isLinux [
              pkgs.autoPatchelfHook
            ];

            buildInputs = pkgs.lib.optionals pkgs.stdenv.isLinux [
              pkgs.glibc
              pkgs.zlib
            ];

            dontUnpack = true;
            installPhase = ''
              mkdir -p $out/bin
              cp $src $out/bin/flamingock
              chmod +x $out/bin/flamingock
            '';
          };
        }
      );
    };
}
```

**Option 2: nixpkgs PR**

Submit a PR to [NixOS/nixpkgs](https://github.com/NixOS/nixpkgs) with a package derivation similar to the flake above.

### CI/CD Integration

- **Flake**: Update hashes in `flake.nix` on each release, commit to repo
- **nixpkgs**: Submit version bump PRs (similar to Homebrew Core)

### Ongoing Maintenance

- **Per release**: Update version and hashes. ~10 minutes.
- **NixOS specifics**: Monitor for changes in `autoPatchelfHook` behavior or required libraries
- Nix expression language evolves — occasional syntax updates may be needed

### User Experience

```bash
# With flake (one-time run)
nix run github:flamingock/flamingock

# With flake (install to profile)
nix profile install github:flamingock/flamingock

# With nixpkgs (once accepted)
nix-env -iA nixpkgs.flamingock

# With NixOS configuration.nix
environment.systemPackages = with pkgs; [ flamingock ];
```

### Cost

**$0**

### Pros

- Growing rapidly in the DevOps community
- Reproducible builds and environments
- Flake provides a self-hosted distribution without waiting for nixpkgs acceptance
- Works on NixOS, macOS, and any Linux with Nix installed
- Can be used in CI/CD via `nix develop` for reproducible environments

### Cons

- **Small but growing audience** (~5% of DevOps practitioners, growing fast)
- Nix expression language has a steep learning curve for maintainers
- `autoPatchelfHook` is required for GraalVM binaries on NixOS — must test carefully
- nixpkgs PR review can take weeks
- Not suitable as a primary distribution channel

### Comparable Tools

| Tool | In nixpkgs? |
|------|------------|
| kubectl | Yes |
| terraform | Yes |
| gh | Yes |
| ripgrep | Yes |

### Audience Fit

DevOps engineers and developers using NixOS or Nix for environment management. **Niche but growing** — worth considering for community credibility.

---

## Option G: Install Script (curl | bash)

### Description

A **shell install script** provides a `curl | bash` one-liner that automates the full install process: platform detection, download, SHA256 verification, placement on PATH, and post-install verification. This is the most universal approach — it works on any Linux distribution (and macOS) with no dependencies beyond `curl` and `bash`.

### Setup Steps

1. **Write `install.sh`**:
   ```bash
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

     # Resolve latest version
     if [ "$VERSION" = "latest" ]; then
       VERSION=$(curl -fsSL "https://api.github.com/repos/flamingock/flamingock/releases/latest" \
         | grep '"tag_name"' | sed -E 's/.*"v([^"]+)".*/\1/')
     fi

     local BINARY="flamingock-${VERSION}-${OS}-${ARCH}"
     local URL="https://github.com/flamingock/flamingock/releases/download/v${VERSION}/${BINARY}"
     local CHECKSUMS_URL="https://github.com/flamingock/flamingock/releases/download/v${VERSION}/SHA256SUMS.txt"

     echo "Installing Flamingock CLI v${VERSION} (${OS}/${ARCH})..."

     # Download to temp directory
     local TMP_DIR
     TMP_DIR="$(mktemp -d)"
     trap 'rm -rf "$TMP_DIR"' EXIT

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

   install_flamingock
   ```
2. **Host the script**: In the repository (e.g., `scripts/install.sh`) and optionally at a vanity URL
3. **Security best practices**:
   - Wrap everything in a function (prevents partial download execution)
   - Use `set -euo pipefail`
   - Never silently `sudo` — ask or inform the user
   - Verify checksums before executing anything
   - Clean up temp files via `trap`

### CI/CD Integration

The script itself requires no CI changes — it reads from existing GitHub Release artifacts. Test the script in CI across distributions (Ubuntu, Fedora, Alpine, Debian) to ensure compatibility.

### Ongoing Maintenance

- **Minimal** — script auto-resolves latest version
- Update if artifact naming conventions change
- Test periodically on new distributions

### User Experience

```bash
# One-liner install
curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# Specific version
FLAMINGOCK_VERSION=1.1.0 curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# Custom directory (no sudo)
FLAMINGOCK_INSTALL_DIR=~/.local/bin curl -fsSL https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.sh | bash

# With vanity URL
curl -fsSL https://get.flamingock.io | bash
```

### Cost

**$0** (or ~$10/year for a vanity domain)

### Pros

- **Universal** — works on any Linux distro, macOS, WSL
- Zero dependencies beyond `curl` and `bash`
- SHA256 verification for supply chain security
- Works in CI/CD pipelines and Docker containers
- Familiar pattern — widely used and accepted
- Same script works for macOS and Linux
- Can be set up in a few hours

### Cons

- `curl | bash` has security perception issues (mitigated by checksums and HTTPS)
- No automatic upgrades
- No clean uninstall (manual `rm`)
- May require `sudo` for `/usr/local/bin`
- Users must trust the script source

### Comparable Tools

| Tool | Install command |
|------|----------------|
| rustup | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` |
| Homebrew | `curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh \| bash` |
| Deno | `curl -fsSL https://deno.land/install.sh \| sh` |
| Helm | `curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \| bash` |
| Docker | `curl -fsSL https://get.docker.com \| sh` |

### Audience Fit

Everyone. CI/CD pipelines, Docker containers, servers, workstations. **Best universal starting point**.

---

## Option H: Direct Download Improvements

### Description

Improve the trust and verifiability of the existing direct download approach without changing the distribution mechanism. This includes:
- **GPG signing** of `SHA256SUMS.txt` (or individual binaries)
- **Sigstore/cosign** as a modern alternative to GPG
- **Better documentation** for manual installation
- **Verification instructions** in release notes

### Setup Steps

**GPG Signing:**
1. Generate a dedicated GPG key for release signing
2. Publish the public key to `flamingock.io/gpg.key` and key servers
3. Sign `SHA256SUMS.txt` in the release workflow:
   ```bash
   gpg --armor --detach-sign SHA256SUMS.txt
   # Produces SHA256SUMS.txt.asc
   ```
4. Upload `SHA256SUMS.txt.asc` as a release artifact

**Sigstore/cosign (modern alternative):**
1. Install `cosign` in CI
2. Sign release artifacts:
   ```bash
   cosign sign-blob --yes \
     --output-signature SHA256SUMS.txt.sig \
     --output-certificate SHA256SUMS.txt.pem \
     SHA256SUMS.txt
   ```
3. Upload `.sig` and `.pem` as release artifacts
4. Sigstore uses keyless signing (OIDC identity from GitHub Actions) — no key management

**Documentation improvements:**
- Add "Verify your download" section to install docs
- Include platform-specific instructions in GitHub Release notes
- Add a `SECURITY.md` explaining the signing process

### CI/CD Integration

Add signing step after generating `SHA256SUMS.txt` in the release workflow. Both GPG and cosign can run in the existing Linux CI job.

### Ongoing Maintenance

- **GPG**: Key rotation and management (same key as APT/YUM if applicable)
- **Cosign**: Nearly zero — keyless signing uses ephemeral keys tied to CI identity
- Update documentation when processes change

### User Experience

```bash
# Download and verify (GPG)
curl -LO https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-linux-x86_64
curl -LO https://github.com/flamingock/flamingock/releases/download/v1.1.0/SHA256SUMS.txt
curl -LO https://github.com/flamingock/flamingock/releases/download/v1.1.0/SHA256SUMS.txt.asc

# Import public key and verify signature
curl -fsSL https://flamingock.io/gpg.key | gpg --import
gpg --verify SHA256SUMS.txt.asc SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt --ignore-missing

# Verify (cosign)
cosign verify-blob \
  --signature SHA256SUMS.txt.sig \
  --certificate SHA256SUMS.txt.pem \
  --certificate-identity "https://github.com/flamingock/flamingock/.github/workflows/release.yml@refs/tags/v1.1.0" \
  --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
  SHA256SUMS.txt
```

### Cost

**$0**

### Pros

- Strengthens supply chain security for all distribution methods
- Cosign requires zero key management (keyless signing)
- Builds trust with security-conscious users and enterprises
- Foundation for APT/YUM repository signing (reuse GPG key)
- Low effort to implement

### Cons

- Most users won't manually verify signatures
- GPG has poor UX for end users
- Cosign is still relatively new (but adopted by Kubernetes, Sigstore ecosystem)
- Doesn't improve the install UX itself

### Comparable Tools

| Tool | Signing method |
|------|---------------|
| Kubernetes | Cosign (Sigstore) |
| Helm | GPG |
| Docker | Content Trust (Notary) |
| HashiCorp | GPG |

### Audience Fit

Security-conscious organizations, enterprise environments with supply chain requirements. **Good foundation** that benefits all other distribution methods.

---

## Comparison Matrix

| Criterion | A. APT | B. YUM/DNF | C. Snap | D. Homebrew | E. AUR | F. Nix | G. Script | H. Signing |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **Setup effort** | High | High | Medium | Low | Low | Medium | Low | Low |
| **Cost** | $0-15/mo | $0-15/mo | $0 | $0 | $0 | $0 | $0 | $0 |
| **Auto-upgrade** | Yes | Yes | Yes | Yes | Yes | Yes | No | N/A |
| **Distro coverage** | Debian/Ubuntu | RHEL/Fedora | Ubuntu+ | Any | Arch | NixOS | Any | Any |
| **Server-friendly** | Yes | Yes | No | No | No | Yes | Yes | N/A |
| **CI/CD friendly** | Yes | Yes | No | No | No | Yes | Yes | N/A |
| **Maintenance** | Medium | Medium | Low | Low | Low | Medium | Minimal | Minimal |
| **Audience reach** | High | High | Medium | Medium | Low | Low | Highest | N/A |
| **sudo required** | Yes | Yes | Yes | No | Yes | No | Maybe | No |
| **Covers macOS too** | No | No | No | Yes | No | Yes | Yes | Yes |

---

## Suggested Progression Path

### Phase 1 — Immediate (Week 1-2)

**G. Install Script + H. Direct Download Improvements**

- Install script provides the universal `curl | bash` experience for all Linux distros (and macOS)
- Cosign/GPG signing of `SHA256SUMS.txt` strengthens supply chain security immediately
- Both are low effort and zero cost
- Install script serves as the "always works" fallback for all platforms

### Phase 2 — When Server Deployments Grow (~3-6 months)

**A. APT Repository + B. YUM/DNF Repository**

- APT covers Debian/Ubuntu (most common Linux servers and CI environments)
- YUM covers RHEL/CentOS/Amazon Linux (enterprise and AWS)
- Together they cover ~90% of Linux server environments
- Implement together since they share tooling (fpm, GPG key, hosting)
- Use the same GPG key from Phase 1

### Phase 3 — When Homebrew Tap Exists for macOS

**D. Homebrew / Linuxbrew**

- If macOS Homebrew tap is already set up, extending to Linux is nearly zero effort
- Covers Linux developers on workstations who prefer Homebrew
- Not a replacement for APT/YUM on servers

### Phase 4 — Community Building (~6-12 months)

**E. AUR + F. Nix**

- AUR is trivial to set up and generates Arch community goodwill
- Nix flake is moderate effort but appeals to a growing DevOps audience
- Both are low priority but high impact per effort for community credibility

### Deprioritized

- **C. Snap** — Classic confinement approval is uncertain, the format is controversial in the Linux community, and the audience overlap with other options is high. Only pursue if there's specific demand from Ubuntu users.
