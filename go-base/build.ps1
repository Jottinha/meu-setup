# Build script - compila o base.exe e copia para os resources do backend.
# Pre-requisitos:
#   - Go 1.23+
#   - go install github.com/tc-hib/go-winres@latest (instalado sob demanda)
#   - hmac.key na raiz do projeto (gerado por scripts/gen-hmac-key.ps1)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Build do MeuSetup Base Installer ===" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$rootKey = Join-Path $projectRoot "hmac.key"
$localKey = Join-Path $scriptDir "hmac.key"
$backendKey = Join-Path $projectRoot "backend\src\main\resources\hmac.key"
$backendResDir = Join-Path $projectRoot "backend\src\main\resources"

# Garante que existe uma chave HMAC na raiz
if (-not (Test-Path $rootKey)) {
    Write-Host "hmac.key nao encontrado. Gerando..." -ForegroundColor Yellow
    & (Join-Path $projectRoot "scripts\gen-hmac-key.ps1")
    if ($LASTEXITCODE -ne 0) { Write-Host "Falha ao gerar hmac.key." -ForegroundColor Red; exit 1 }
}

# Garante go-winres
if (-not (Get-Command go-winres -ErrorAction SilentlyContinue)) {
    Write-Host "go-winres nao encontrado. Instalando..." -ForegroundColor Yellow
    go install github.com/tc-hib/go-winres@latest
}

# Copia hmac.key para go-base/ (para go:embed) e para backend/resources/
Write-Host "Copiando hmac.key para os builds..." -ForegroundColor Yellow
Copy-Item -Path $rootKey -Destination $localKey -Force
if (-not (Test-Path $backendResDir)) { New-Item -ItemType Directory -Path $backendResDir -Force | Out-Null }
Copy-Item -Path $rootKey -Destination $backendKey -Force

try {
    Write-Host "Gerando recursos Windows (manifesto UAC)..." -ForegroundColor Yellow
    go-winres make
    if ($LASTEXITCODE -ne 0) { Write-Host "Falha no go-winres." -ForegroundColor Red; exit 1 }

    Write-Host "Compilando executavel Windows x64..." -ForegroundColor Yellow
    $env:GOOS   = "windows"
    $env:GOARCH = "amd64"
    $env:CGO_ENABLED = "0"
    go build -trimpath -ldflags="-s -w" -o base.exe .
    if ($LASTEXITCODE -ne 0) { Write-Host "Falha na compilacao Go." -ForegroundColor Red; exit 1 }

    $dest = Join-Path $backendResDir "base.exe"
    Write-Host "Copiando base.exe para $dest ..." -ForegroundColor Yellow
    Copy-Item -Path base.exe -Destination $dest -Force
}
finally {
    # Remove a copia local da chave (a backend mantem a sua para o Spring carregar)
    if (Test-Path $localKey) { Remove-Item $localKey -Force }
}

Write-Host ""
Write-Host "Build concluido! base.exe gerado e copiado." -ForegroundColor Green
Write-Host ""
