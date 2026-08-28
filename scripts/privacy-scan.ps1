[CmdletBinding()]
param(
    [switch]$TrackedOnly,
    [switch]$History
)

$ErrorActionPreference = 'Stop'
$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) { throw '请在 Git 仓库内运行此脚本' }
Set-Location $repoRoot

$findings = [System.Collections.Generic.List[object]]::new()
$binaryExtensions = @(
    '.class', '.jar', '.war', '.zip', '.gz', '.tgz', '.7z', '.png', '.jpg', '.jpeg',
    '.gif', '.webp', '.ico', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
    '.ttf', '.otf', '.woff', '.woff2', '.xdb'
)

function Add-Finding([string]$category, [string]$path, [int]$line, [string]$reason) {
    $findings.Add([PSCustomObject]@{
        Category = $category
        File = $path
        Line = $line
        Reason = $reason
    })
}

function Test-Placeholder([string]$value, [string]$line) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $true }
    if ($line -match '(?i)@Value\(|dummy-bcrypt-hash|"integrity"\s*:') { return $true }
    return $value -match '^(\$\{|%[A-Za-z_][A-Za-z0-9_]*%|\$env:|\$[A-Za-z_]|process\.env|<[^>]+>|your|example|placeholder|dummy|change[-_]?me|any_long|!BOOTSTRAP_REQUIRED!|this\.|props\.|form\.|dto\.)'
}

function Test-PublicIp([string]$candidate) {
    $parts = @($candidate.Split('.') | ForEach-Object { [int]$_ })
    if ($parts.Count -ne 4 -or ($parts | Where-Object { $_ -lt 0 -or $_ -gt 255 })) { return $false }
    if ($parts[0] -in @(0, 10, 127)) { return $false }
    if ($parts[0] -eq 169 -and $parts[1] -eq 254) { return $false }
    if ($parts[0] -eq 172 -and $parts[1] -ge 16 -and $parts[1] -le 31) { return $false }
    if ($parts[0] -eq 192 -and $parts[1] -eq 168) { return $false }
    # RFC 5737 文档保留地址允许出现在示例中。
    if ($parts[0] -eq 192 -and $parts[1] -eq 0 -and $parts[2] -eq 2) { return $false }
    if ($parts[0] -eq 198 -and $parts[1] -eq 51 -and $parts[2] -eq 100) { return $false }
    if ($parts[0] -eq 203 -and $parts[1] -eq 0 -and $parts[2] -eq 113) { return $false }
    return $true
}

function Scan-Lines([string]$displayPath, [string[]]$lines, [int]$baseLine = 0) {
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = [string]$lines[$i]
        $lineNo = $baseLine + $i + 1

        if ($line -match '-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----') {
            Add-Finding 'private-key' $displayPath $lineNo '发现私钥头'
        }
        if ($line -notmatch '"integrity"\s*:' -and $line -match '(?i)(AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-(proj-)?[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|AIza[0-9A-Za-z_-]{30,}|sk_live_[0-9A-Za-z]{16,})') {
            Add-Finding 'known-token' $displayPath $lineNo '发现已知令牌前缀'
        }
        if ($line -match 'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}') {
            Add-Finding 'jwt' $displayPath $lineNo '发现 JWT 字面量'
        }
        if ($line -notmatch '(?i)dummy-bcrypt-hash' -and $line -match '\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}') {
            Add-Finding 'bcrypt-literal' $displayPath $lineNo '发现可用于登录验证的 BCrypt 字面量'
        }
        if ($line -match '(?i)\b(https?|jdbc:[a-z0-9]+)://[^\s/@:]+:[^\s/@]+@') {
            Add-Finding 'url-credential' $displayPath $lineNo 'URL 中包含用户名和密码'
        }

        $assignments = [regex]::Matches($line, '(?i)(password|passwd|pwd|secret|api[-_]?key|access[-_]?key|client[-_]?secret|auth[-_]?token|private[-_]?key)\s*[:=]\s*(["'']?)([^\s"'',;)}]+)')
        foreach ($assignment in $assignments) {
            $quote = $assignment.Groups[2].Value
            $value = $assignment.Groups[3].Value
            $isCodeReference = $displayPath -match '(?i)\.(java|js|ts|vue)$' `
                    -and [string]::IsNullOrEmpty($quote) `
                    -and $value -match '^[A-Za-z_$][A-Za-z0-9_.$]*(\([^)]*\))?$'
            if ($isCodeReference) { continue }
            if (-not (Test-Placeholder $value $line) -and $value.Length -ge 8) {
                Add-Finding 'credential-literal' $displayPath $lineNo ('疑似硬编码 ' + $assignment.Groups[1].Value)
            }
        }

        foreach ($mail in [regex]::Matches($line, '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b')) {
            if ($mail.Value -notmatch '(?i)@(example\.(com|org|net)|test\.com)$') {
                Add-Finding 'email' $displayPath $lineNo '发现非示例域名邮箱'
            }
        }

        # 只检查 URL/host/proxy/server/ssh 上下文，避免把 SVG 路径的小数误判成 IP。
        if ($line -match '(?i)(https?://|host|proxy|server|ssh|scp|remote)') {
            foreach ($ip in [regex]::Matches($line, '(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])')) {
                if (Test-PublicIp $ip.Value) {
                    Add-Finding 'public-ip' $displayPath $lineNo '发现公网 IP 字面量'
                }
            }
        }
    }
}

$files = if ($TrackedOnly) {
    @(git -c core.quotepath=false ls-files)
} else {
    @(git -c core.quotepath=false ls-files --cached --others --exclude-standard)
}

foreach ($file in $files) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue }
    if ([IO.Path]::GetExtension($file).ToLowerInvariant() -in $binaryExtensions) { continue }
    if ((Get-Item -LiteralPath $file).Length -gt 5MB) { continue }
    try { Scan-Lines $file @(Get-Content -LiteralPath $file -ErrorAction Stop) } catch {}
}

if ($History) {
    $historyPatterns = @(
        '-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----',
        '(AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-(proj-)?[A-Za-z0-9_-]{20,})',
        '[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}',
        '(password|passwd|pwd|secret|api[-_]?key|access[-_]?key|client[-_]?secret|auth[-_]?token)[[:space:]]*[:=][[:space:]]*',
        '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}',
        '([0-9]{1,3}\.){3}[0-9]{1,3}'
    )
    $seenHistory = @{}
    foreach ($commit in @(git rev-list --all)) {
        foreach ($pattern in $historyPatterns) {
            foreach ($hit in @(& git grep -n -I -i -E -e $pattern $commit -- 2>$null)) {
                if ($hit -notmatch '^([0-9a-f]+):(.+?):([0-9]+):(.*)$') { continue }
                $path = $Matches[2]
                $lineNo = [int]$Matches[3]
                $line = $Matches[4]
                $key = $path + ':' + $lineNo + ':' + $line
                if ($seenHistory.ContainsKey($key)) { continue }
                $seenHistory[$key] = $true
                Scan-Lines ($commit.Substring(0, 8) + ':' + $path) @($line) ($lineNo - 1)
            }
        }
    }
}

if ($findings.Count -gt 0) {
    $unique = @($findings | Sort-Object Category, File, Line -Unique)
    $unique | Format-Table -AutoSize
    Write-Host ("隐私扫描失败：发现 {0} 个候选项。输出仅含位置，不打印敏感值。" -f $unique.Count) -ForegroundColor Red
    exit 1
}

Write-Host ("隐私扫描通过：已检查 {0} 个文件，未发现未豁免的敏感字面量。" -f $files.Count)
