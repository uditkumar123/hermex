# Changelog Generator Script
# Generates CHANGELOG.md from conventional commits
# Usage: .\scripts\generate-changelog.ps1

param(
    [Parameter(Mandatory=$false)]
    [string]$OutputFile = "CHANGELOG.md",
    
    [Parameter(Mandatory=$false)]
    [int]$MaxCommits = 50
)

# Get the latest tag or use first commit
$lastTag = git describe --tags --abbrev=0 2>$null
if ($LASTEXITCODE -ne 0) {
    $lastTag = git rev-list --max-parents=0 HEAD
}

Write-Host "Generating changelog from $lastTag..." -ForegroundColor Cyan

# Get commits since last tag
$commits = git log "$lastTag..HEAD" --pretty=format:"%h|%s|%an|%ad" --date=short -n $MaxCommits

if (-not $commits) {
    Write-Host "No new commits found" -ForegroundColor Yellow
    exit 0
}

# Parse commits by type
$features = @()
$fixes = @()
$perf = @()
$docs = @()
$refactor = @()
$other = @()

foreach ($line in $commits -split "`n") {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    
    $parts = $line -split "\|", 4
    $hash = $parts[0]
    $message = $parts[1]
    $author = $parts[2]
    $date = $parts[3]
    
    # Parse conventional commit type
    if ($message -match "^feat(?:\(.+\))?:\s*(.+)") {
        $features += "- $($Matches[1]) ($hash) - $author"
    }
    elseif ($message -match "^fix(?:\(.+\))?:\s*(.+)") {
        $fixes += "- $($Matches[1]) ($hash) - $author"
    }
    elseif ($message -match "^perf(?:\(.+\))?:\s*(.+)") {
        $perf += "- $($Matches[1]) ($hash) - $author"
    }
    elseif ($message -match "^docs(?:\(.+\))?:\s*(.+)") {
        $docs += "- $($Matches[1]) ($hash) - $author"
    }
    elseif ($message -match "^refactor(?:\(.+\))?:\s*(.+)") {
        $refactor += "- $($Matches[1]) ($hash) - $author"
    }
    else {
        $other += "- $message ($hash) - $author"
    }
}

# Generate changelog content
$date = Get-Date -Format "yyyy-MM-dd"
$version = (Get-Content "android\app\build.gradle.kts" | Select-String 'versionName\s*=\s*"(.+)"').Matches.Groups[1].Value

$changelog = @"
# Changelog

## [$version] - $date

### Features
$($features -join "`n")

### Bug Fixes
$($fixes -join "`n")

### Performance
$($perf -join "`n")

### Documentation
$($docs -join "`n")

### Refactoring
$($refactor -join "`n")

### Other Changes
$($other -join "`n")

"@

# Check if changelog exists and prepend
if (Test-Path $OutputFile) {
    $existing = Get-Content $OutputFile -Raw
    $changelog = $changelog + "`n---`n`n" + $existing
}

Set-Content $OutputFile $changelog

Write-Host "Changelog generated: $OutputFile" -ForegroundColor Green
Write-Host "Features: $($features.Count), Fixes: $($fixes.Count)" -ForegroundColor Gray
