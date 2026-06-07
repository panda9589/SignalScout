$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = Join-Path $repoRoot ".tools\jdk-21"
$envLoader = Join-Path $PSScriptRoot "load-env.ps1"

if (-not (Test-Path -LiteralPath $javaHome)) {
    & (Join-Path $PSScriptRoot "install-dev-tools.ps1")
}

if (Test-Path -LiteralPath $envLoader) {
    . $envLoader
    Import-SignalScoutEnv -Path (Join-Path $repoRoot ".env.local")
    Import-SignalScoutEnv -Path (Join-Path $repoRoot "config\ingestion.local.env")
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

& (Join-Path $repoRoot "backend\gradlew.bat") -p (Join-Path $repoRoot "backend") bootRun
