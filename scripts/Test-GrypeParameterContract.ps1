param(
    [Parameter(Mandatory=$true)][string]$Executable,
    [Parameter(Mandatory=$true)][string]$SourceFile
)
$ErrorActionPreference = 'Stop'
$commands = @(
    '', 'config', 'config locations', 'db', 'db check', 'db delete',
    'db diff', 'db import', 'db list', 'db providers', 'db search',
    'db search vuln', 'db status', 'db update', 'explain', 'version',
    'completion', 'help'
)
$actual = [System.Collections.Generic.HashSet[string]]::new()
foreach ($command in $commands) {
    $tokens = if ($command) { @($command.Split(' ')) } else { @() }
    $help = (& $Executable @tokens --help 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw "Help failed for grype $command" }
    foreach ($line in ($help -split '\r?\n')) {
        if ($line -match '^\s*(?:-[A-Za-z0-9],\s*)?--([a-z][a-z0-9-]*)(?:\s|$)') {
            [void]$actual.Add($Matches[1])
        }
    }
}
$source = Get-Content $SourceFile -Raw
if ($source -notmatch '(?m)DOCUMENTED = \[([^\]]+)\]') { throw 'Cannot find documented flag inventory' }
$inventory = @([regex]::Matches($Matches[1], "'([a-z][a-z0-9-]*)'") | ForEach-Object { $_.Groups[1].Value })
$missing = @($inventory | Where-Object { -not $actual.Contains($_) })
$unexpected = @($actual | Where-Object { $_ -notin $inventory })
Write-Host "Native unique flags: $($actual.Count); plugin documented flags: $($inventory.Count)"
Write-Host "Native flags: $(@($actual | Sort-Object) -join ', ')"
if ($missing.Count -or $unexpected.Count) { throw "CLI contract drift. Missing in native CLI: $($missing -join ', '); missing in plugin inventory: $($unexpected -join ', ')" }
Write-Host 'Native flag inventory matches.'
