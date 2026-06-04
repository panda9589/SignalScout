param(
    [string]$ToolsDir = ".tools"
)

$ErrorActionPreference = "Stop"

function Expand-ZipToSingleDirectory {
    param(
        [string]$ZipPath,
        [string]$Destination,
        [string]$MarkerFile
    )

    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }

    $temp = Join-Path ([System.IO.Path]::GetTempPath()) ([System.Guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Force -Path $temp | Out-Null
    try {
        Expand-Archive -LiteralPath $ZipPath -DestinationPath $temp -Force
        $root = Get-ChildItem -LiteralPath $temp | Where-Object { $_.PSIsContainer } | Select-Object -First 1
        if (-not $root) {
            throw "Archive $ZipPath did not contain a root directory"
        }
        Move-Item -LiteralPath $root.FullName -Destination $Destination
        if (-not (Test-Path -LiteralPath (Join-Path $Destination $MarkerFile))) {
            throw "Install verification failed for $Destination"
        }
    }
    finally {
        if (Test-Path -LiteralPath $temp) {
            Remove-Item -LiteralPath $temp -Recurse -Force
        }
    }
}

$repoRoot = Resolve-Path "."
$toolsPath = Join-Path $repoRoot $ToolsDir
$downloadsPath = Join-Path $toolsPath "downloads"
New-Item -ItemType Directory -Force -Path $downloadsPath | Out-Null

$jdkZip = Join-Path $downloadsPath "temurin-jdk21.zip"
$nodeZip = Join-Path $downloadsPath "node.zip"
$gradleZip = Join-Path $downloadsPath "gradle.zip"

$jdkUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
$nodeUrl = "https://nodejs.org/dist/v22.11.0/node-v22.11.0-win-x64.zip"
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"

Write-Host "Downloading Temurin JDK 21..."
Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip
Expand-ZipToSingleDirectory -ZipPath $jdkZip -Destination (Join-Path $toolsPath "jdk-21") -MarkerFile "bin\java.exe"

Write-Host "Downloading Node.js..."
Invoke-WebRequest -Uri $nodeUrl -OutFile $nodeZip
Expand-ZipToSingleDirectory -ZipPath $nodeZip -Destination (Join-Path $toolsPath "node") -MarkerFile "node.exe"

Write-Host "Downloading Gradle..."
Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip
Expand-ZipToSingleDirectory -ZipPath $gradleZip -Destination (Join-Path $toolsPath "gradle") -MarkerFile "bin\gradle.bat"

Write-Host "Installed local tools:"
& (Join-Path $toolsPath "jdk-21\bin\java.exe") -version
& (Join-Path $toolsPath "node\node.exe") --version
& (Join-Path $toolsPath "node\npm.cmd") --version
& (Join-Path $toolsPath "gradle\bin\gradle.bat") --version
