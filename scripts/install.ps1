#!/usr/bin/env pwsh
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
        $Release = Invoke-RestMethod "https://api.github.com/repos/flamingock/flamingock-cli/releases/latest"
        $Version = $Release.tag_name -replace '^v', ''
    }

    $Binary = "flamingock-$Version-windows-x86_64.exe"
    $Url = "https://github.com/flamingock/flamingock-cli/releases/download/v$Version/$Binary"
    $ChecksumsUrl = "https://github.com/flamingock/flamingock-cli/releases/download/v$Version/SHA256SUMS.txt"

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
        $ExpectedHash = ((Get-Content $ChecksumsPath | Where-Object { $_ -match $Binary }) -replace '\s+.*', '').ToLower()
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
