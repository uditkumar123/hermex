# Local Debug APK Script
# This script bumps the version and builds a local debug APK. Public GitHub Releases are built by .github/workflows/version-bump.yml as signed release APKs.
# Usage: .\scripts\build-apk.ps1 [major|minor|patch]

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("major", "minor", "patch")]
    [string]$BumpType = "patch"
)

$androidDir = "android"
$scriptsDir = "scripts"

# Check if we're in the right directory
if (-not (Test-Path $androidDir)) {
    Write-Error "Please run this script from the project root directory"
    exit 1
}

# Bump version
Write-Host "Bumping version ($BumpType)..." -ForegroundColor Cyan
& ".\$scriptsDir\bump-version.ps1" -BumpType $BumpType

if ($LASTEXITCODE -ne 0) {
    Write-Error "Version bump failed"
    exit 1
}

# Build the APK
Write-Host "Building APK..." -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$gradleBat = Get-ChildItem "C:\Users\kumar\.gradle\wrapper\dists\gradle-8.9-bin\*\gradle-8.9\bin\gradle.bat" | Select-Object -First 1

if (-not $gradleBat) {
    Write-Error "Gradle not found"
    exit 1
}

Push-Location $androidDir
& $gradleBat.FullName assembleDebug --no-daemon
$buildResult = $LASTEXITCODE
Pop-Location

if ($buildResult -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "APK location: android\app\build\outputs\apk\debug\app-debug.apk"
} else {
    Write-Error "Build failed"
    exit 1
}
