#Requires -Version 5.1
<#
Only removes reproducible local outputs from an explicit allowlist.
Default: preview. Use -Apply to remove, or -Apply -WhatIf for a dry run.
Source files, installed dependencies, uploads, local configuration and design
evidence are never included. Close project services and QA browsers first.
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param([switch]$Apply)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path.TrimEnd('\', '/')
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'blog-server/pom.xml')) -or
    -not (Test-Path -LiteralPath (Join-Path $projectRoot 'blog-web/package.json'))) {
    throw 'Run this script from the Blog Java Vue repository.'
}
$separator = [IO.Path]::DirectorySeparatorChar
$allowedPaths = @(
    'out',
    'blog-server/build',
    'blog-server/META-INF',
    'blog-server/org',
    'blog-server/target',
    'blog-web/dist',
    'blog-web/node_modules/.vite',
    'blog-web/node_modules/.vite-temp',
    'logs'
)

if ($Apply -and -not $WhatIfPreference -and (Get-Command Get-CimInstance -ErrorAction SilentlyContinue)) {
    $activeProcesses = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^(java|node|chrome|msedge)(\.exe)?$' -and
        $_.CommandLine -and $_.CommandLine.IndexOf($projectRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0
    })
    if ($activeProcesses.Count) {
        throw ('Close project services/QA browsers first. PIDs: ' + ($activeProcesses.ProcessId -join ', '))
    }
}

# Validate all paths before performing any deletion. Never follow directory links.
$validatedTargets = foreach ($relativePath in $allowedPaths) {
    $candidate = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $candidate)) { continue }
    $resolvedPath = (Resolve-Path -LiteralPath $candidate).Path
    if (-not $resolvedPath.StartsWith($projectRoot + $separator, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing path outside repository: $resolvedPath"
    }
    $ancestorPath = Split-Path -Parent $resolvedPath
    while ($ancestorPath -ne $projectRoot) {
        if (-not $ancestorPath -or
            -not $ancestorPath.StartsWith($projectRoot + $separator, [StringComparison]::OrdinalIgnoreCase) -or
            ((Get-Item -LiteralPath $ancestorPath -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Refusing linked or unexpected parent: $ancestorPath"
        }
        $ancestorPath = Split-Path -Parent $ancestorPath
    }
    $entry = Get-Item -LiteralPath $resolvedPath -Force
    if (-not $entry.PSIsContainer) { throw "Expected a generated directory: $resolvedPath" }
    if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw "Refusing linked directory: $resolvedPath"
    }
    $descendants = @(Get-ChildItem -LiteralPath $resolvedPath -Force -Recurse)
    if (@($descendants | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count) {
        throw "Refusing directory containing links: $resolvedPath"
    }
    $files = @($descendants | Where-Object { -not $_.PSIsContainer })
    [pscustomobject]@{
        Path = $resolvedPath
        Files = $files.Count
        MiB = [math]::Round(($files | Measure-Object Length -Sum).Sum / 1MB, 2)
    }
}

$validatedTargets | Format-Table -AutoSize
if (-not $Apply) {
    Write-Host 'Preview only. After reviewing, run with -Apply to remove these outputs.'
    return
}
foreach ($target in $validatedTargets) {
    $currentPath = (Resolve-Path -LiteralPath $target.Path).Path
    if ($currentPath -ne $target.Path -or
        -not $currentPath.StartsWith($projectRoot + $separator, [StringComparison]::OrdinalIgnoreCase) -or
        ((Get-Item -LiteralPath $currentPath -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw "Target changed since validation: $($target.Path)"
    }
    if ($PSCmdlet.ShouldProcess($currentPath, 'Remove generated directory')) {
        Remove-Item -LiteralPath $currentPath -Recurse -Force
    }
}
