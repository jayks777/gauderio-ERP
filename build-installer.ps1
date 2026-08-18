$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$TargetDir = Join-Path $ProjectRoot "target"
$LibDir = Join-Path $TargetDir "lib"
$AppDir = Join-Path $TargetDir "app"
$OutputDir = Join-Path $TargetDir "installer"

$JPackage = "C:\Program Files\Java\jdk-26.0.1\bin\jpackage.exe"

$Jar = Join-Path $TargetDir "gauderio-erp-1.0-SNAPSHOT.jar"

Write-Host "========================================"
Write-Host "       GAUDERIO-ERP - BUILD"
Write-Host "========================================"
Write-Host ""

# ========================================
# Validações
# ========================================

if (-not (Test-Path $JPackage)) {
    throw "jpackage não encontrado em: $JPackage"
}

if (-not (Test-Path $Jar)) {
    throw "JAR da aplicação não encontrado: $Jar"
}

if (-not (Test-Path $LibDir)) {
    throw "Diretório de dependências não encontrado: $LibDir"
}

# ========================================
# Limpeza
# ========================================

Write-Host "[1/4] Limpando diretórios..."

if (Test-Path $AppDir) {
    Remove-Item $AppDir -Recurse -Force
}

if (Test-Path $OutputDir) {
    Remove-Item $OutputDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $AppDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# ========================================
# Preparação
# ========================================

Write-Host "[2/4] Preparando aplicação..."

Copy-Item `
    -Path $Jar `
    -Destination $AppDir

$Dependencies = Get-ChildItem `
    -Path $LibDir `
    -Filter "*.jar" `
    -File

foreach ($Dependency in $Dependencies) {

    Write-Host "  Copiando: $($Dependency.Name)"

    Copy-Item `
        -Path $Dependency.FullName `
        -Destination $AppDir
}

# ========================================
# JPackage
# ========================================

Write-Host ""
Write-Host "[3/4] Criando instalador..."
Write-Host ""

& $JPackage `
    --type exe `
    --name "Gauderio-ERP" `
    --app-version "1.0.0" `
    --vendor "Jaykson Bolico" `
    --description "Sistema financeiro Gauderio-ERP" `
    --input $AppDir `
    --main-jar "gauderio-erp-1.0-SNAPSHOT.jar" `
    --main-class "br.com.gauderio.Main" `
    --dest $OutputDir `
    --win-dir-chooser `
    --win-menu `
    --win-shortcut `
    --win-menu-group "Gauderio-ERP" `
    --win-per-user-install

# ========================================
# Finalização
# ========================================

Write-Host ""
Write-Host "[4/4] Finalizado!"
Write-Host ""

Write-Host "========================================"
Write-Host "       INSTALADOR GERADO"
Write-Host "========================================"
Write-Host ""

Get-ChildItem $OutputDir

Write-Host ""
Write-Host "Localização:"
Write-Host $OutputDir