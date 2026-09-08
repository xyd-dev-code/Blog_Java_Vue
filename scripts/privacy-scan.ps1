[CmdletBinding()]
param([switch]$TrackedOnly, [switch]$History, [switch]$Staged)
# Compatibility entry point. Python reads Git objects and omits secret values.
$ErrorActionPreference = 'Stop'
$runtime = $null
foreach ($candidate in @('python3', 'python')) {
    if (Get-Command $candidate -ErrorAction SilentlyContinue) {
        & $candidate -c 'import sys; sys.exit(sys.version_info < (3, 11))' 2>$null
        if ($LASTEXITCODE -eq 0) { $runtime = $candidate; break }
    }
}
if (-not $runtime) { throw 'Privacy check blocked: install Python 3.11+.' }
$scanner = Join-Path $PSScriptRoot 'privacy_guard.py'
$scanArgs = @('worktree')
if ($Staged) { $scanArgs = @('staged') }
elseif ($TrackedOnly) { $scanArgs += '--tracked-only' }
& $runtime $scanner @scanArgs
$scanExit = $LASTEXITCODE
if ($History) {
    & $runtime $scanner history
    if ($LASTEXITCODE -ne 0) { $scanExit = $LASTEXITCODE }
}
exit $scanExit
