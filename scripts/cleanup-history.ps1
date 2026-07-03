# cleanup-history.ps1
# Removes AI/agent tooling files from entire git history.
#
# WARNING: This rewrites history. After running, you MUST force push.
# All collaborators will need to re-clone or git reset --hard origin/master.
#
# Usage: .\scripts\cleanup-history.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$files = @(
    ".agy/config.json"
    ".agy/rules.md"
    ".planning/PROJECT.md"
    ".planning/REQUIREMENTS.md"
    ".planning/ROADMAP.md"
    ".planning/STATE.md"
    ".planning/v1.0-ROADMAP.md"
    ".planning/v1.0-STATE.md"
    ".planning/v2.0-ROADMAP.md"
    ".planning/v2.0-STATE.md"
    ".xcodebuildmcp/config.yaml"
    "AGENTS.md"
    "CLAUDE.md"
    "docs/agents/domain.md"
    "docs/agents/feature-gap-index.md"
    "docs/agents/issue-tracker.md"
    "docs/agents/multi-server-state-isolation.md"
    "docs/agents/triage-labels.md"
    "docs/agents/upstream-smoke-checklist.md"
)

Write-Host "============================================" -ForegroundColor Yellow
Write-Host " Git History Cleanup — AI File Removal" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "This will rewrite ALL commits to remove:" -ForegroundColor Cyan
foreach ($f in $files) { Write-Host "  - $f" }
Write-Host ""

$confirm = Read-Host "Type YES to proceed"
if ($confirm -ne "YES") {
    Write-Host "Aborted." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[1/3] Backing up current branch..." -ForegroundColor Green
$backup = "backup-before-cleanup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
git branch $backup
Write-Host "  Backup branch: $backup"

Write-Host ""
Write-Host "[2/3] Rewriting history (this may take a minute)..." -ForegroundColor Green

$indexFilter = "git rm --cached --ignore-unmatch " + ($files | ForEach-Object { "`"$_`"" }) -join " "

git filter-branch --force --index-filter $indexFilter HEAD

Write-Host ""
Write-Host "[3/3] Cleaning up refs..." -ForegroundColor Green
git update-ref -d refs/original/refs/heads/master
git reflog expire --expire=now --all
git gc --prune=now --aggressive

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " Done! History rewritten." -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. git add .gitignore"
Write-Host "  2. git commit -m 'chore: remove AI tooling from repo'"
Write-Host "  3. git push origin master --force"
Write-Host ""
Write-Host "All collaborators must re-clone or run:" -ForegroundColor Yellow
Write-Host "  git fetch origin && git reset --hard origin/master"
Write-Host ""
Write-Host "To delete the backup branch later:" -ForegroundColor Yellow
Write-Host "  git branch -D $backup"
