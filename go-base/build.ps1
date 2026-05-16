# Build script — compila o base.exe e copia para os resources do backend
# Pré-requisito: go install github.com/tc-hib/go-winres@latest

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Build do MeuSetup Base Installer ===" -ForegroundColor Cyan

# Verifica se go-winres está instalado
if (-not (Get-Command go-winres -ErrorAction SilentlyContinue)) {
    Write-Host "go-winres não encontrado. Instalando..." -ForegroundColor Yellow
    go install github.com/tc-hib/go-winres@latest
}

# Gera o arquivo de recursos Windows (.syso com o manifesto UAC)
Write-Host "Gerando recursos Windows (manifesto UAC)..." -ForegroundColor Yellow
go-winres make
if ($LASTEXITCODE -ne 0) { Write-Host "Falha no go-winres." -ForegroundColor Red; exit 1 }

# Compila o executável Windows x64
Write-Host "Compilando executável Windows x64..." -ForegroundColor Yellow
$env:GOOS   = "windows"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "0"
go build -o base.exe .
if ($LASTEXITCODE -ne 0) { Write-Host "Falha na compilação Go." -ForegroundColor Red; exit 1 }

# Copia para os resources do backend
$dest = "..\backend\src\main\resources\base.exe"
Write-Host "Copiando base.exe para $dest ..." -ForegroundColor Yellow
Copy-Item -Path base.exe -Destination $dest -Force

Write-Host ""
Write-Host "Build concluído! base.exe gerado e copiado." -ForegroundColor Green
Write-Host ""
