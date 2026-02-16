# Windows Distribution Strategy

> Decision document for Flamingock CLI Windows distribution options.
> Current state: native binary (`flamingock-X.Y.Z-windows-x86_64.exe`) published to GitHub Releases with SHA256 checksums.
> Manual install: download `.exe`, place in a directory on PATH.

---

## Option A: Scoop

### Description

[Scoop](https://scoop.sh/) is a command-line installer for Windows that installs programs to the user's home directory — **no administrator privileges required**. Packages are defined as JSON manifests in "buckets" (Git repositories). Scoop is popular among Windows developers who want a Unix-like package management experience.

Distribution paths:
- **Custom bucket** (like a Homebrew tap) — immediate, full control
- **Main bucket** — official Scoop repository, requires meeting submission criteria
- **Extras bucket** — broader acceptance criteria than Main

For Flamingock, a **custom bucket** is the best starting point.

### Setup Steps

1. **Create a public GitHub repository**: `flamingock/scoop-flamingock`
2. **Add a manifest** `bucket/flamingock.json`:
   ```json
   {
     "version": "1.1.0",
     "description": "CLI for audited, synchronized evolution of distributed systems",
     "homepage": "https://docs.flamingock.io/cli",
     "license": "Apache-2.0",
     "url": "https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-windows-x86_64.exe#/flamingock.exe",
     "hash": "<SHA256_HASH>",
     "bin": "flamingock.exe",
     "checkver": {
       "github": "https://github.com/flamingock/flamingock"
     },
     "autoupdate": {
       "url": "https://github.com/flamingock/flamingock/releases/download/v$version/flamingock-$version-windows-x86_64.exe#/flamingock.exe",
       "hash": {
         "url": "https://github.com/flamingock/flamingock/releases/download/v$version/SHA256SUMS.txt",
         "regex": "$sha256\\s+flamingock-$version-windows-x86_64\\.exe"
       }
     }
   }
   ```
3. **Key features of the manifest**:
   - `#/flamingock.exe` renames the downloaded file (Scoop URL fragment convention)
   - `checkver` auto-detects new versions from GitHub releases
   - `autoupdate` defines how to construct the URL and extract hashes for new versions
   - Scoop automatically adds the binary to the user's PATH

### CI/CD Integration

Two approaches:
1. **Manual with Scoop's autoupdate**: Run `scoop update` locally or in CI — Scoop's `checkver` and `autoupdate` fields handle version bumps automatically. Use [Scoop's `excavator`](https://github.com/ScoopInstaller/Excavator) bot for automated updates.
2. **Automated via release workflow**: Add a step that updates the manifest JSON and pushes to the bucket repo:
   ```yaml
   - name: Update Scoop manifest
     run: |
       VERSION="${{ github.event.inputs.version }}"
       HASH=$(grep "windows-x86_64.exe" SHA256SUMS.txt | awk '{print $1}')
       # Update version and hash in manifest JSON
       # Push to scoop-flamingock repo
   ```

### Ongoing Maintenance

- **Per release**: Update manifest (version + hash). Automatable via `autoupdate`.
- **Very low maintenance** — Scoop manifests are simple JSON files
- Monitor Scoop manifest schema changes (rare)

### User Experience

```powershell
# Add bucket (first time)
scoop bucket add flamingock https://github.com/flamingock/scoop-flamingock

# Install
scoop install flamingock

# Verify
flamingock --version

# Upgrade
scoop update flamingock

# Uninstall
scoop uninstall flamingock
```

No Administrator prompt. No UAC dialog. Binary is immediately available on PATH.

### Cost

**$0**

### Pros

- **No administrator privileges required** — installs to `~/scoop/`
- Automatic PATH management
- Built-in `checkver`/`autoupdate` for low-maintenance updates
- Clean install/uninstall — no registry entries, no system modifications
- Popular among Windows developers and DevOps practitioners
- Can be set up in an afternoon
- Simple JSON manifest (easier to maintain than Chocolatey `.nuspec`)

### Cons

- Smaller user base than Chocolatey (~30% of Windows dev tool users vs ~50% for Chocolatey)
- Requires Scoop to be installed first (but Scoop itself is easy to install)
- Custom bucket requires `scoop bucket add` (extra step)
- Less enterprise adoption than Chocolatey or winget
- No GUI — command-line only (which is fine for CLI tool users)

### Comparable Tools

| Tool | Scoop bucket |
|------|-------------|
| Terraform | `main` bucket |
| kubectl | `main` bucket |
| gh (GitHub CLI) | `main` bucket |
| ripgrep | `main` bucket |
| jq | `main` bucket |

### Audience Fit

Windows developers who prefer command-line package management without admin privileges. **Primary Windows audience** for developer tools.

---

## Option B: Chocolatey

### Description

[Chocolatey](https://chocolatey.org/) is the largest Windows package manager with over 10,000 packages. Packages are defined in NuGet format (`.nupkg` with `.nuspec` metadata and optional PowerShell install scripts). Chocolatey is widely used in enterprise environments and supports both CLI and GUI (Chocolatey GUI) installation.

Packages are published to the [Chocolatey Community Repository](https://community.chocolatey.org/packages) and go through a **moderation queue** (1-7 days for review).

### Setup Steps

1. **Create a Chocolatey account** at [community.chocolatey.org](https://community.chocolatey.org)
2. **Get an API key** from your account profile
3. **Create the package structure**:
   ```
   flamingock/
   ├── flamingock.nuspec
   └── tools/
       └── chocolateyinstall.ps1
   ```
4. **Write `flamingock.nuspec`**:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <package xmlns="http://schemas.chocolatey.org/2015/06/nuspec.xsd">
     <metadata>
       <id>flamingock</id>
       <version>1.1.0</version>
       <title>Flamingock CLI</title>
       <authors>Flamingock</authors>
       <owners>flamingock</owners>
       <projectUrl>https://docs.flamingock.io/cli</projectUrl>
       <licenseUrl>https://github.com/flamingock/flamingock/blob/master/LICENSE</licenseUrl>
       <requireLicenseAcceptance>false</requireLicenseAcceptance>
       <description>CLI for audited, synchronized evolution of distributed systems. Execute, validate, dry-run, and audit Flamingock changes from the command line.</description>
       <tags>flamingock cli devops database migration change-management</tags>
       <packageSourceUrl>https://github.com/flamingock/flamingock</packageSourceUrl>
       <docsUrl>https://docs.flamingock.io/cli</docsUrl>
       <bugTrackerUrl>https://github.com/flamingock/flamingock/issues</bugTrackerUrl>
     </metadata>
   </package>
   ```
5. **Write `tools/chocolateyinstall.ps1`**:
   ```powershell
   $ErrorActionPreference = 'Stop'
   $toolsDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

   $packageArgs = @{
     packageName    = 'flamingock'
     url64bit       = 'https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-windows-x86_64.exe'
     checksum64     = '<SHA256_HASH>'
     checksumType64 = 'sha256'
     fileFullPath   = Join-Path $toolsDir 'flamingock.exe'
   }
   Get-ChocolateyWebFile @packageArgs
   ```
6. **Build and push**:
   ```powershell
   choco pack
   choco push flamingock.1.1.0.nupkg --source https://push.chocolatey.org/ --api-key <API_KEY>
   ```

### CI/CD Integration

Add a Windows job to the release workflow:
1. Install Chocolatey in the runner
2. Build the `.nupkg` with updated version and checksum
3. Push to Chocolatey with the API key (stored as GitHub Secret)
4. **Note**: Moderation queue means the package won't be available immediately (1-7 days)

```yaml
- name: Push to Chocolatey
  env:
    CHOCO_API_KEY: ${{ secrets.CHOCOLATEY_API_KEY }}
  run: |
    choco pack
    choco push flamingock.${{ env.VERSION }}.nupkg --source https://push.chocolatey.org/ --api-key $env:CHOCO_API_KEY
```

### Ongoing Maintenance

- **Per release**: Update `.nuspec` version and install script checksum, push. ~15 minutes if automated.
- **Moderation**: Each version goes through review (1-7 days). Trusted maintainers get faster reviews over time.
- **Package verification**: Chocolatey runs automated checks (virus scan, install test). May require fixes if checks fail.
- Monitor Chocolatey API and packaging changes

### User Experience

```powershell
# Install (requires Administrator)
choco install flamingock

# Upgrade
choco upgrade flamingock

# Uninstall
choco uninstall flamingock
```

### Cost

**$0** for community repository. Chocolatey for Business (C4B) is $17+/user/month but not needed for open-source distribution.

### Pros

- **Largest Windows package ecosystem** — highest discoverability
- Widely used in enterprise environments
- Automatic virus scanning during moderation
- Clean install/upgrade/uninstall lifecycle
- Familiar to Windows administrators
- Supports both CLI and GUI management

### Cons

- **Requires Administrator privileges** for install
- **Moderation queue** (1-7 days) delays availability of new versions
- More complex packaging than Scoop (NuGet format, PowerShell scripts)
- Moderation can reject packages for various reasons
- Enterprise users often use private Chocolatey servers (may not see community packages)

### Comparable Tools

| Tool | Chocolatey package |
|------|-------------------|
| Git | `git` |
| Node.js | `nodejs` |
| Terraform | `terraform` |
| Docker Desktop | `docker-desktop` |
| GitHub CLI | `gh` |

### Audience Fit

Windows administrators and enterprise environments. **Important for enterprise adoption** where Chocolatey is the standard software management tool.

---

## Option C: winget (Windows Package Manager)

### Description

[winget](https://github.com/microsoft/winget-cli) is **Microsoft's official package manager**, pre-installed on Windows 11 and available on Windows 10 (via App Installer update). Packages are defined as YAML manifests in the [`microsoft/winget-pkgs`](https://github.com/microsoft/winget-pkgs) community repository.

winget is growing rapidly and is becoming the default package manager for Windows. It supports both interactive and silent installs, making it suitable for both end users and automation.

### Setup Steps

1. **Create manifest files** (winget uses a multi-file manifest format):
   ```
   manifests/f/Flamingock/Flamingock.CLI/1.1.0/
   ├── Flamingock.Flamingock.CLI.yaml          # Version manifest
   ├── Flamingock.Flamingock.CLI.installer.yaml # Installer manifest
   └── Flamingock.Flamingock.CLI.locale.en-US.yaml # Locale manifest
   ```

2. **`Flamingock.Flamingock.CLI.yaml`** (version manifest):
   ```yaml
   PackageIdentifier: Flamingock.Flamingock.CLI
   PackageVersion: 1.1.0
   DefaultLocale: en-US
   ManifestType: version
   ManifestVersion: 1.6.0
   ```

3. **`Flamingock.Flamingock.CLI.installer.yaml`**:
   ```yaml
   PackageIdentifier: Flamingock.Flamingock.CLI
   PackageVersion: 1.1.0
   InstallerType: portable
   Commands:
     - flamingock
   InstallerSwitches:
     Custom: --version
   Installers:
     - Architecture: x64
       InstallerUrl: https://github.com/flamingock/flamingock/releases/download/v1.1.0/flamingock-1.1.0-windows-x86_64.exe
       InstallerSha256: <SHA256_HASH>
   ManifestType: installer
   ManifestVersion: 1.6.0
   ```

4. **`Flamingock.Flamingock.CLI.locale.en-US.yaml`**:
   ```yaml
   PackageIdentifier: Flamingock.Flamingock.CLI
   PackageVersion: 1.1.0
   PackageLocale: en-US
   Publisher: Flamingock
   PublisherUrl: https://flamingock.io
   PackageName: Flamingock CLI
   PackageUrl: https://docs.flamingock.io/cli
   License: Apache-2.0
   LicenseUrl: https://github.com/flamingock/flamingock/blob/master/LICENSE
   ShortDescription: CLI for audited, synchronized evolution of distributed systems
   Description: >-
     Flamingock CLI enables executing Flamingock operations outside the normal
     application lifecycle. Execute, validate, dry-run, rollback, and audit
     changes from the command line.
   Tags:
     - cli
     - devops
     - database
     - migration
     - change-management
   ManifestType: defaultLocale
   ManifestVersion: 1.6.0
   ```

5. **Submit PR** to `microsoft/winget-pkgs`

### CI/CD Integration

Automate version bumps using [`wingetcreate`](https://github.com/microsoft/winget-create):
```yaml
- name: Update winget manifest
  run: |
    wingetcreate update Flamingock.Flamingock.CLI \
      --version ${{ env.VERSION }} \
      --urls "https://github.com/flamingock/flamingock/releases/download/v${{ env.VERSION }}/flamingock-${{ env.VERSION }}-windows-x86_64.exe" \
      --submit \
      --token ${{ secrets.WINGET_PAT }}
```

`wingetcreate` generates the manifest files and submits a PR automatically.

### Ongoing Maintenance

- **Per release**: Run `wingetcreate update` to submit a version bump PR. ~5 minutes if automated.
- **Review latency**: First submission takes **1-3 weeks** for review. Subsequent updates are faster (hours to days) once established.
- Monitor winget manifest schema changes (evolves with new versions)

### User Experience

```powershell
# Install
winget install Flamingock.Flamingock.CLI

# Upgrade
winget upgrade Flamingock.Flamingock.CLI

# Uninstall
winget uninstall Flamingock.Flamingock.CLI

# Search
winget search flamingock
```

### Cost

**$0**

### Pros

- **Pre-installed on Windows 11** — no additional software needed
- Microsoft-backed — growing rapidly, becoming the default
- Discoverable via `winget search`
- Clean manifest format (YAML)
- `wingetcreate` automates version bumps
- Both interactive and silent install modes
- No administrator privileges needed for `portable` type installs
- Highest credibility signal for Windows distribution

### Cons

- **Slow initial review** (1-3 weeks for first submission)
- Not pre-installed on older Windows 10 builds
- `portable` installer type is relatively new — may have quirks
- Less mature ecosystem than Chocolatey (fewer packages, newer tooling)
- Manifest format is more verbose than Scoop JSON

### Comparable Tools

| Tool | winget ID |
|------|----------|
| VS Code | `Microsoft.VisualStudioCode` |
| Git | `Git.Git` |
| Node.js | `OpenJS.NodeJS` |
| GitHub CLI | `GitHub.cli` |
| Terraform | `Hashicorp.Terraform` |

### Audience Fit

All Windows users, especially those on Windows 11. **Increasingly important** as winget becomes the standard Windows package manager.

---

## Option D: Windows Code Signing (Authenticode)

### Description

**Authenticode** is Microsoft's code signing technology for Windows executables. Without a valid signature, Windows SmartScreen may block the executable with a "Windows protected your PC" warning, especially for new/unsigned executables.

Certificate types:
- **OV (Organization Validation)**: $200-500/year from CAs (DigiCert, Sectigo, GlobalSign). Requires business identity verification. New OV certificates start with **zero SmartScreen reputation** — users still see warnings until enough installs accumulate (weeks to months of "reputation warm-up").
- **EV (Extended Validation)**: $300-600/year. Requires hardware security module (HSM) for key storage. Provides **immediate SmartScreen trust** — no warm-up period. Historically required physical USB token; now available via cloud HSM services.
- **Azure Trusted Signing**: ~$120/year (Azure consumption pricing). Microsoft's new signing service. Provides **EV-equivalent trust** (immediate SmartScreen acceptance) without managing your own HSM. Requires Azure subscription and identity verification.

### Setup Steps

**Option 1: Traditional OV/EV Certificate**

1. **Purchase a code signing certificate** from a CA (e.g., DigiCert, Sectigo)
2. **For EV**: Set up HSM (hardware token or cloud HSM like Azure Key Vault, AWS CloudHSM)
3. **Sign the executable** in CI:
   ```powershell
   # With signtool (Windows SDK)
   signtool sign /f certificate.pfx /p "$PASSWORD" /tr http://timestamp.digicert.com /td sha256 /fd sha256 flamingock.exe

   # Verify
   signtool verify /pa flamingock.exe
   ```

**Option 2: Azure Trusted Signing**

1. **Create an Azure account** and set up Azure Trusted Signing resource
2. **Complete identity verification** (organization validation)
3. **Configure certificate profile** in Azure portal
4. **Sign in CI** using Azure CLI:
   ```yaml
   - uses: azure/trusted-signing-action@v0.5.0
     with:
       azure-tenant-id: ${{ secrets.AZURE_TENANT_ID }}
       azure-client-id: ${{ secrets.AZURE_CLIENT_ID }}
       azure-client-secret: ${{ secrets.AZURE_CLIENT_SECRET }}
       endpoint: https://eus.codesigning.azure.net/
       trusted-signing-account-name: flamingock
       certificate-profile-name: flamingock-cli
       files-folder: ./dist
       files-folder-filter: exe
   ```

### CI/CD Integration

Add a signing step to the Windows build job in `release.yml`:

**For OV/EV with PFX:**
1. Store certificate as base64-encoded GitHub Secret
2. Decode and import in CI
3. Sign with `signtool`

**For Azure Trusted Signing:**
1. Store Azure credentials as GitHub Secrets
2. Use the official GitHub Action
3. Signing happens via API call (no local certificate needed)

**Timing**: Signing adds ~30 seconds to the build. No external review or approval needed.

### Ongoing Maintenance

| Certificate type | Renewal | Key management |
|-----------------|---------|---------------|
| OV | Annual ($200-500) | PFX file or HSM |
| EV | Annual ($300-600) | HSM required (hardware token or cloud) |
| Azure Trusted Signing | Monthly (~$10/mo) | Managed by Azure |

- Certificate renewal is annual (or continuous for Azure Trusted Signing)
- CI secrets must be rotated on renewal
- Monitor for changes in Windows SmartScreen policies

### User Experience

**Without signing (current):**
```
⚠ Windows protected your PC
Microsoft Defender SmartScreen prevented an unrecognized app from starting.
Running this app might put your PC at risk.
                                        [Don't run] [Run anyway]
                                        (hidden under "More info")
```

**With OV signing (new certificate):**
```
⚠ Windows protected your PC
The publisher could not be verified. Are you sure you want to run this software?
Publisher: Flamingock (verified)
                                        [Don't run] [Run]
```
SmartScreen warning persists until reputation builds (weeks/months of downloads).

**With EV signing or Azure Trusted Signing:**
No SmartScreen warning. Binary runs immediately. Professional publisher information shown in file properties.

### Cost

| Option | Initial cost | Annual cost |
|--------|-------------|-------------|
| OV certificate | $200-500 | $200-500 |
| EV certificate | $300-600 + HSM setup | $300-600 + HSM costs |
| Azure Trusted Signing | Azure account setup | ~$120/year |

### Pros

- Eliminates SmartScreen warnings (EV/Azure: immediately; OV: after warm-up)
- Professional credibility — publisher name shown in Windows security dialogs
- Required for enterprise environments with strict software policies
- Prerequisite for Chocolatey and winget (they prefer/require signed binaries)
- Timestamp ensures signature remains valid after certificate expiry

### Cons

- **Ongoing cost** ($120-600/year depending on option)
- OV certificates require reputation warm-up (SmartScreen still warns initially)
- EV certificates require HSM management
- Certificate/secret management in CI
- Identity verification process (1-7 days)
- Azure Trusted Signing is relatively new (~2024)

### Comparable Tools

Virtually every professionally distributed Windows binary is Authenticode-signed: Visual Studio Code, Git for Windows, Node.js, Docker Desktop, all Microsoft products, all Adobe products.

### Audience Fit

All Windows users. **Essential for professional distribution** — unsigned binaries face significant friction from SmartScreen.

---

## Option E: MSI Installer

### Description

An **MSI (Microsoft Installer) package** provides a standard Windows installation experience: double-click GUI install, Add/Remove Programs integration, system PATH setup, and compatibility with enterprise deployment tools (SCCM, Intune, Group Policy).

MSI packages are built with [WiX Toolset](https://wixtoolset.org/) (open-source) or commercial tools like InstallShield.

### Setup Steps

1. **Install WiX Toolset** (v4+) in CI
2. **Create WiX project** (`flamingock.wxs`):
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <Wix xmlns="http://wixtoolset.org/schemas/v4/wxs">
     <Package Name="Flamingock CLI"
              Version="1.1.0"
              Manufacturer="Flamingock"
              UpgradeCode="PUT-GUID-HERE"
              InstallerVersion="500">

       <MajorUpgrade DowngradeErrorMessage="A newer version is already installed." />

       <StandardDirectory Id="ProgramFiles64Folder">
         <Directory Id="INSTALLFOLDER" Name="Flamingock">
           <Component Id="MainExecutable" Guid="PUT-GUID-HERE">
             <File Source="flamingock.exe" KeyPath="yes" />
           </Component>
           <Component Id="PathEntry" Guid="PUT-GUID-HERE">
             <Environment Id="PATH" Name="PATH" Value="[INSTALLFOLDER]"
                          Permanent="no" Part="last" Action="set" System="yes" />
           </Component>
         </Directory>
       </StandardDirectory>

       <Feature Id="Main" Level="1">
         <ComponentRef Id="MainExecutable" />
         <ComponentRef Id="PathEntry" />
       </Feature>
     </Package>
   </Wix>
   ```
3. **Build**: `wix build flamingock.wxs -o flamingock-1.1.0.msi`
4. **Sign the MSI** with Authenticode (Option D)
5. Upload as a release artifact

### CI/CD Integration

Add to the Windows build job:
1. Install WiX Toolset
2. Build MSI after native image compilation
3. Sign MSI with Authenticode
4. Upload as release artifact

```yaml
- name: Build MSI
  run: |
    dotnet tool install --global wix
    wix build flamingock.wxs -o flamingock-${{ env.VERSION }}.msi
- name: Sign MSI
  run: |
    signtool sign /f certificate.pfx /p "$PASSWORD" /tr http://timestamp.digicert.com /td sha256 /fd sha256 flamingock-${{ env.VERSION }}.msi
```

### Ongoing Maintenance

- **Per release**: Rebuild MSI with new binary. Automated in CI.
- Keep `UpgradeCode` GUID **constant** across versions (enables upgrade detection)
- Update WiX syntax for major WiX version changes (v4 → v5 etc.)
- Test on current Windows versions

### User Experience

```powershell
# GUI install (double-click)
# OR silent install (command line / scripting)
msiexec /i flamingock-1.1.0.msi /quiet

# Verify
flamingock --version

# Upgrade (in-place via MajorUpgrade)
msiexec /i flamingock-1.2.0.msi /quiet

# Uninstall (via Settings > Apps, or command line)
msiexec /x flamingock-1.1.0.msi /quiet
```

### Cost

**$0** (WiX is open-source). Requires Authenticode certificate for signing (see Option D).

### Pros

- Standard Windows installation experience
- Add/Remove Programs integration
- Automatic PATH setup
- `MajorUpgrade` handles in-place upgrades cleanly
- Compatible with enterprise deployment (SCCM, Intune, GPO)
- Silent install mode for automation (`/quiet`)
- Clean uninstall (removes files, PATH entry)

### Cons

- **Overkill for a single-exe CLI tool** — MSI is designed for complex applications
- Requires Administrator privileges
- WiX has a steep learning curve (XML-heavy)
- Adds complexity to the CI pipeline
- No automatic update mechanism (must download new MSI)
- Most CLI tool users prefer package managers (Scoop/Chocolatey/winget)
- Enterprise use case (SCCM/Intune) is rare for developer CLI tools

### Comparable Tools

| Tool | Uses MSI? |
|------|----------|
| AWS CLI v2 | Yes (primary Windows distribution) |
| Node.js | Yes |
| Git for Windows | Yes (via Inno Setup, similar concept) |
| Most modern CLI tools | No — prefer Scoop/Chocolatey/winget |

### Audience Fit

Enterprise IT departments deploying via SCCM/Intune/GPO. **Not recommended** as a priority for Flamingock CLI given the developer-focused audience.

---

## Option F: PowerShell Install Script

### Description

A **PowerShell install script** provides a one-liner install experience for Windows: `irm https://get.flamingock.io/install.ps1 | iex`. The script downloads the binary, verifies the SHA256 checksum, installs to a user-writable directory (e.g., `%LOCALAPPDATA%\flamingock\bin`), and adds it to the user's PATH — all without Administrator privileges.

This is the Windows equivalent of `curl | bash` on Unix systems.

### Setup Steps

1. **Write `install.ps1`**:
   ```powershell
   #!/usr/bin/env pwsh
   # Flamingock CLI Installer for Windows
   $ErrorActionPreference = 'Stop'

   function Install-Flamingock {
       $Version = if ($env:FLAMINGOCK_VERSION) { $env:FLAMINGOCK_VERSION } else { "latest" }
       $InstallDir = if ($env:FLAMINGOCK_INSTALL_DIR) {
           $env:FLAMINGOCK_INSTALL_DIR
       } else {
           Join-Path $env:LOCALAPPDATA "flamingock\bin"
       }

       # Resolve latest version
       if ($Version -eq "latest") {
           $Release = Invoke-RestMethod "https://api.github.com/repos/flamingock/flamingock/releases/latest"
           $Version = $Release.tag_name -replace '^v', ''
       }

       $Binary = "flamingock-$Version-windows-x86_64.exe"
       $Url = "https://github.com/flamingock/flamingock/releases/download/v$Version/$Binary"
       $ChecksumsUrl = "https://github.com/flamingock/flamingock/releases/download/v$Version/SHA256SUMS.txt"

       Write-Host "Installing Flamingock CLI v$Version..."

       # Create install directory
       if (-not (Test-Path $InstallDir)) {
           New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
       }

       $TempDir = Join-Path $env:TEMP "flamingock-install-$(Get-Random)"
       New-Item -ItemType Directory -Path $TempDir -Force | Out-Null

       try {
           # Download binary and checksums
           $BinaryPath = Join-Path $TempDir "flamingock.exe"
           $ChecksumsPath = Join-Path $TempDir "SHA256SUMS.txt"

           Invoke-WebRequest -Uri $Url -OutFile $BinaryPath -UseBasicParsing
           Invoke-WebRequest -Uri $ChecksumsUrl -OutFile $ChecksumsPath -UseBasicParsing

           # Verify SHA256
           $ExpectedHash = (Get-Content $ChecksumsPath | Where-Object { $_ -match $Binary }) -replace '\s+.*', ''
           $ActualHash = (Get-FileHash -Path $BinaryPath -Algorithm SHA256).Hash.ToLower()

           if ($ExpectedHash -ne $ActualHash) {
               Write-Error "SHA256 checksum mismatch!`nExpected: $ExpectedHash`nActual: $ActualHash"
               return
           }

           Write-Host "SHA256 checksum verified."

           # Install
           $DestPath = Join-Path $InstallDir "flamingock.exe"
           Move-Item -Path $BinaryPath -Destination $DestPath -Force

           # Add to PATH if not already present
           $CurrentPath = [Environment]::GetEnvironmentVariable("Path", "User")
           if ($CurrentPath -notlike "*$InstallDir*") {
               [Environment]::SetEnvironmentVariable("Path", "$CurrentPath;$InstallDir", "User")
               Write-Host "Added $InstallDir to user PATH."
               Write-Host "Restart your terminal for PATH changes to take effect."
           }

           Write-Host "Flamingock CLI v$Version installed to $DestPath"

           # Verify (use full path since PATH may not be updated in current session)
           & $DestPath --version
       }
       finally {
           Remove-Item -Path $TempDir -Recurse -Force -ErrorAction SilentlyContinue
       }
   }

   Install-Flamingock
   ```

2. **Host the script**: In the repository (e.g., `scripts/install.ps1`) and optionally at a vanity URL
3. **Security considerations**:
   - PowerShell execution policy may block scripts — `irm | iex` bypasses this for inline execution
   - SHA256 verification before installation
   - Installs to user directory — no admin required
   - Clean temp file handling with `try/finally`

### CI/CD Integration

No CI changes needed — the script reads from existing GitHub Release artifacts. Test the script in CI on Windows runners.

### Ongoing Maintenance

- **Minimal** — script auto-resolves latest version
- Update if artifact naming conventions change
- Test on new Windows versions and PowerShell updates

### User Experience

```powershell
# One-liner install (PowerShell)
irm https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.ps1 | iex

# Specific version
$env:FLAMINGOCK_VERSION="1.1.0"; irm https://raw.githubusercontent.com/flamingock/flamingock/master/scripts/install.ps1 | iex

# With vanity URL
irm https://get.flamingock.io/install.ps1 | iex

# Uninstall (manual)
Remove-Item "$env:LOCALAPPDATA\flamingock" -Recurse -Force
# Remove from PATH manually or via:
$path = [Environment]::GetEnvironmentVariable("Path", "User") -replace [regex]::Escape(";$env:LOCALAPPDATA\flamingock\bin"), ""
[Environment]::SetEnvironmentVariable("Path", $path, "User")
```

### Cost

**$0** (or ~$10/year for a vanity domain)

### Pros

- **No Administrator privileges required** — installs to user directory
- No package manager needed — works on any Windows system with PowerShell
- Automatic PATH setup
- SHA256 verification for security
- Familiar pattern — used by many modern tools
- Works in CI/CD pipelines and automation scripts
- Same vanity URL can serve both bash (Linux/macOS) and PowerShell (Windows) scripts

### Cons

- `irm | iex` has security perception issues (same as `curl | bash`)
- No automatic upgrade mechanism
- Manual uninstall
- PowerShell execution policy may confuse some users (though `irm | iex` bypasses it)
- SmartScreen may still warn on the downloaded `.exe` if unsigned (combine with Option D)

### Comparable Tools

| Tool | Install command |
|------|----------------|
| Deno | `irm https://deno.land/install.ps1 \| iex` |
| Bun | `irm bun.sh/install.ps1 \| iex` |
| Starship | `irm https://starship.rs/install.ps1 \| iex` |
| Rust/rustup | `irm https://win.rustup.rs -OutFile rustup-init.exe; .\rustup-init.exe` |

### Audience Fit

Windows developers and CI/CD pipelines. Good complement to Scoop/winget. **Best quick-start option** for Windows.

---

## Comparison Matrix

| Criterion | A. Scoop | B. Chocolatey | C. winget | D. Code Signing | E. MSI | F. PS Script |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|
| **Setup effort** | Low | Medium | Medium | Medium | High | Low |
| **Cost** | $0 | $0 | $0 | $120-600/yr | $0* | $0 |
| **Admin required** | No | Yes | No** | N/A | Yes | No |
| **Auto-upgrade** | Yes | Yes | Yes | N/A | No | No |
| **Discoverability** | Low | High | High | N/A | N/A | Low |
| **SmartScreen bypass** | Yes | Yes | Yes | Yes | Yes | No*** |
| **Enterprise-ready** | Partial | Yes | Yes | Yes | Yes | No |
| **Maintenance** | Low | Medium | Low | Low | Medium | Minimal |
| **Review/moderation** | None | 1-7 days | 1-3 weeks | N/A | N/A | None |
| **Audience reach** | Medium | High | Growing | All | Low | High |

\* MSI itself is free (WiX is open-source) but requires Authenticode certificate for signing.
\** `portable` type doesn't require admin; `exe`/`msi` types do.
\*** The downloaded `.exe` may trigger SmartScreen; combine with Option D to resolve.

---

## Suggested Progression Path

### Phase 1 — Immediate (Week 1-2)

**F. PowerShell Install Script + A. Scoop**

- PowerShell script provides the universal `irm | iex` experience with zero dependencies
- Scoop provides proper package management without admin — perfect for developers
- Both are zero cost and can be set up quickly
- No moderation queues — available immediately

### Phase 2 — When Adoption Justifies Cost (~3-6 months)

**D. Windows Code Signing (Azure Trusted Signing)**

- Eliminates SmartScreen warnings for all distribution methods
- Azure Trusted Signing at ~$120/year is the best value (EV-equivalent trust, no HSM)
- Prerequisite for professional appearance in Chocolatey and winget
- Required for enterprise environments

### Phase 3 — Broader Package Manager Coverage (~6-12 months)

**C. winget + B. Chocolatey**

- winget is becoming the standard on Windows 11 — submit early to start building presence
- Chocolatey has the largest ecosystem and enterprise adoption
- Both benefit from having signed binaries (Phase 2)
- Submit to winget first (longer review), then Chocolatey

### Deprioritized

- **E. MSI Installer** — Only pursue if there's specific enterprise demand for SCCM/Intune deployment. Overkill for a single-exe CLI tool with a developer audience.
