$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = Join-Path $repoRoot ".tools\jdk-21"

if (-not (Test-Path -LiteralPath $javaHome)) {
    & (Join-Path $PSScriptRoot "install-dev-tools.ps1")
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

& (Join-Path $repoRoot "backend\gradlew.bat") -p (Join-Path $repoRoot "backend") bootRun
