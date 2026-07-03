# Version Bump Hook
# This script bumps the version number in build.gradle.kts before building an APK
# Usage: .\scripts\bump-version.ps1 [major|minor|patch]

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("major", "minor", "patch")]
    [string]$BumpType = "patch"
)

$buildGradlePath = "app\build.gradle.kts"

if (-not (Test-Path $buildGradlePath)) {
    Write-Error "build.gradle.kts not found at $buildGradlePath"
    exit 1
}

# Read the current version
$content = Get-Content $buildGradlePath -Raw

# Extract current versionCode and versionName
$versionCodeMatch = [regex]::Match($content, 'versionCode\s*=\s*(\d+)')
$versionNameMatch = [regex]::Match($content, 'versionName\s*=\s*"(\d+)\.(\d+)\.(\d+)"')

if (-not $versionCodeMatch.Success -or -not $versionNameMatch.Success) {
    Write-Error "Could not parse version from build.gradle.kts"
    exit 1
}

$versionCode = [int]$versionCodeMatch.Groups[1].Value
$major = [int]$versionNameMatch.Groups[1].Value
$minor = [int]$versionNameMatch.Groups[2].Value
$patch = [int]$versionNameMatch.Groups[3].Value

Write-Host "Current version: $major.$minor.$patch (code: $versionCode)"

# Bump version based on type
switch ($BumpType) {
    "major" {
        $major++
        $minor = 0
        $patch = 0
    }
    "minor" {
        $minor++
        $patch = 0
    }
    "patch" {
        $patch++
    }
}

$versionCode++
$newVersion = "$major.$minor.$patch"

Write-Host "New version: $newVersion (code: $versionCode)"

# Update the file
$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $versionCode"
$content = $content -replace 'versionName\s*=\s*"\d+\.\d+\.\d+"', "versionName = `"$newVersion`""

Set-Content $buildGradlePath -Value $content -NoNewline

Write-Host "Version bumped successfully to $newVersion" -ForegroundColor Green

# Return the new version for use in build commands
return $newVersion
