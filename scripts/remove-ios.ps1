# remove-ios.ps1
# Removes all iOS source, build configs, CI workflows, and docs.
# Run this when diverging from the upstream iOS fork.
#
# Usage: .\scripts\remove-ios.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "============================================" -ForegroundColor Yellow
Write-Host " iOS Removal — Android-Only Fork" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "This will permanently delete:" -ForegroundColor Cyan
Write-Host "  - HermesMobile/ (iOS source)"
Write-Host "  - HermesMobileTests/ (iOS tests)"
Write-Host "  - HermesMobile.xcodeproj/ (Xcode project)"
Write-Host "  - HermesShareExtension/ (Share Extension)"
Write-Host "  - HermesLiveActivityWidget/ (Live Activity Widget)"
Write-Host "  - Config/ (xcconfig, export plists)"
Write-Host "  - ci/ (TestFlight scripts)"
Write-Host "  - scripts/check-swift-file-sizes"
Write-Host "  - .github/workflows/pr-ci.yml"
Write-Host "  - .github/workflows/internal-testflight.yml"
Write-Host "  - .github/workflows/external-testflight.yml"
Write-Host "  - .github/workflows/_testflight.yml"
Write-Host "  - iOS sections from docs"
Write-Host ""

$confirm = Read-Host "Type YES to proceed"
if ($confirm -ne "YES") {
    Write-Host "Aborted." -ForegroundColor Red
    exit 1
}

# --- Directories ---
$dirs = @(
    "HermesMobile"
    "HermesMobileTests"
    "HermesMobile.xcodeproj"
    "HermesShareExtension"
    "HermesLiveActivityWidget"
    "Config"
    "ci"
)

foreach ($d in $dirs) {
    if (Test-Path $d) {
        Write-Host "  Removing $d/" -ForegroundColor DarkGray
        git rm -r $d
    }
}

# --- Individual files ---
$files = @(
    "scripts/check-swift-file-sizes"
    ".github/workflows/pr-ci.yml"
    ".github/workflows/internal-testflight.yml"
    ".github/workflows/external-testflight.yml"
    ".github/workflows/_testflight.yml"
    "TESTFLIGHT.md"
)

foreach ($f in $files) {
    if (Test-Path $f) {
        Write-Host "  Removing $f" -ForegroundColor DarkGray
        git rm $f
    }
}

Write-Host ""
Write-Host "Done! Review changes with: git diff --staged --stat" -ForegroundColor Green
Write-Host "Then commit: git commit -m 'chore: remove iOS, Android-only fork'" -ForegroundColor Green
