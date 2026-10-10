[CmdletBinding()]
param(
    [ValidateSet("exe", "msi", "app-image")]
    [string]$Type = "exe"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "target"))
$inputDirectory = Join-Path $targetRoot "jpackage-input"
$outputDirectory = Join-Path $targetRoot "installer"
$applicationJar = "gauderio-erp-1.0.0.jar"

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$InstallHint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando '$Name' nao encontrado. $InstallHint"
    }
}

function Reset-BuildDirectory {
    param([Parameter(Mandatory = $true)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $targetPrefix = $targetRoot.TrimEnd('\') + '\'
    if (-not $resolvedPath.StartsWith($targetPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Diretorio de build fora de target: $resolvedPath"
    }

    if (Test-Path -LiteralPath $resolvedPath) {
        Remove-Item -LiteralPath $resolvedPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $resolvedPath | Out-Null
}

Assert-Command -Name "java" -InstallHint "Instale o JDK 26 e configure JAVA_HOME/PATH."
Assert-Command -Name "mvn" -InstallHint "Instale o Apache Maven 3.9 ou superior."
Assert-Command -Name "jpackage" -InstallHint "Use um JDK completo; o JRE nao contem o jpackage."

$javaVersionText = (& java --version | Select-Object -First 1)
$javaMajorMatch = [regex]::Match($javaVersionText, '\d+')
if (-not $javaMajorMatch.Success -or [int]$javaMajorMatch.Value -lt 26) {
    throw "Este projeto requer JDK 26 ou superior. Encontrado: $javaVersionText"
}

if ($Type -in @("exe", "msi")) {
    $hasWix3 = (Get-Command "candle" -ErrorAction SilentlyContinue) -and
               (Get-Command "light" -ErrorAction SilentlyContinue)
    $hasWix4 = Get-Command "wix" -ErrorAction SilentlyContinue
    if (-not $hasWix3 -and -not $hasWix4) {
        throw "WiX Toolset nao encontrado. Instale WiX 3.14+ ou 4+ e adicione-o ao PATH."
    }
}

Push-Location $projectRoot
try {
    Write-Host "[1/3] Compilando e executando os testes..." -ForegroundColor Cyan
    & mvn clean package
    if ($LASTEXITCODE -ne 0) {
        throw "O Maven encerrou com codigo $LASTEXITCODE."
    }

    $builtJar = Join-Path $targetRoot $applicationJar
    $dependencyDirectory = Join-Path $targetRoot "lib"
    if (-not (Test-Path -LiteralPath $builtJar -PathType Leaf)) {
        throw "JAR principal nao encontrado: $builtJar"
    }
    if (-not (Test-Path -LiteralPath $dependencyDirectory -PathType Container)) {
        throw "Dependencias de runtime nao encontradas: $dependencyDirectory"
    }

    Write-Host "[2/3] Preparando arquivos do aplicativo..." -ForegroundColor Cyan
    Reset-BuildDirectory -Path $inputDirectory
    Reset-BuildDirectory -Path $outputDirectory
    Copy-Item -LiteralPath $builtJar -Destination $inputDirectory
    Get-ChildItem -LiteralPath $dependencyDirectory -Filter "*.jar" -File |
        Copy-Item -Destination $inputDirectory

    $jpackageArguments = @(
        "--type", $Type,
        "--dest", $outputDirectory,
        "--input", $inputDirectory,
        "--name", "Gauderio-ERP",
        "--app-version", "1.0.0",
        "--vendor", "Jaykson Bolico",
        "--description", "Sistema financeiro para controle de contas e movimentacoes",
        "--copyright", "Copyright 2026 Jaykson Bolico",
        "--main-jar", $applicationJar,
        "--main-class", "br.com.gauderio.Launcher",
        "--icon", (Join-Path $projectRoot "favicon.ico"),
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "-Dfile.encoding=UTF-8"
    )

    if ($Type -in @("exe", "msi")) {
        $jpackageArguments += @(
            "--win-per-user-install",
            "--win-dir-chooser",
            "--win-menu",
            "--win-menu-group", "Gauderio-ERP",
            "--win-shortcut",
            "--win-shortcut-prompt",
            "--win-upgrade-uuid", "52500691-1074-4164-bf50-2f9c18e6659d"
        )
    }

    Write-Host "[3/3] Gerando pacote $Type com runtime Java incluido..." -ForegroundColor Cyan
    & jpackage @jpackageArguments
    if ($LASTEXITCODE -ne 0) {
        throw "O jpackage encerrou com codigo $LASTEXITCODE."
    }

    $artifacts = Get-ChildItem -LiteralPath $outputDirectory
    Write-Host "Pacote criado com sucesso:" -ForegroundColor Green
    $artifacts | ForEach-Object { Write-Host "  $($_.FullName)" }
}
finally {
    Pop-Location
}
