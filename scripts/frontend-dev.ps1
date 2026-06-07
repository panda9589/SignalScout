$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$nodeHome = Join-Path $repoRoot ".tools\node"

if (-not (Test-Path -LiteralPath $nodeHome)) {
    & (Join-Path $PSScriptRoot "install-dev-tools.ps1")
}

$env:PATH = "$nodeHome;$env:PATH"

Push-Location (Join-Path $repoRoot "frontend")
try {
    & (Join-Path $nodeHome "npm.cmd") run dev
}
finally {
    Pop-Location
}
