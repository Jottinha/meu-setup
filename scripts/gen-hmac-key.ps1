# Gera a chave HMAC compartilhada entre o backend (Spring) e o base.exe (Go).
# Idempotente: se hmac.key ja existe na raiz, nao faz nada.
# Saidas:
#   - hmac.key  (raiz): 64 chars hex, UTF-8 sem BOM, sem newline. Usado pelos
#                       fluxos locais nao-Docker (go-base\build.ps1 e mvn).
#   - .env      (raiz): HMAC_KEY=<hex>. Lido pelo docker-compose como build arg.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$keyPath = Join-Path $projectRoot "hmac.key"
$envPath = Join-Path $projectRoot ".env"

if (Test-Path $keyPath) {
    Write-Host "hmac.key ja existe em $keyPath (nao sobrescrito)." -ForegroundColor Yellow

    if (-not (Test-Path $envPath)) {
        $existingHex = [System.IO.File]::ReadAllText($keyPath).Trim()
        [System.IO.File]::WriteAllText($envPath, "HMAC_KEY=$existingHex`n", [System.Text.UTF8Encoding]::new($false))
        Write-Host ".env gerado a partir do hmac.key existente." -ForegroundColor Green
    }

    exit 0
}

$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
} finally {
    $rng.Dispose()
}

$hex = -join ($bytes | ForEach-Object { $_.ToString("x2") })

[System.IO.File]::WriteAllText($keyPath, $hex, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($envPath, "HMAC_KEY=$hex`n", [System.Text.UTF8Encoding]::new($false))

Write-Host "hmac.key gerado em $keyPath (64 chars hex)." -ForegroundColor Green
Write-Host ".env gerado em $envPath (usado pelo docker-compose)." -ForegroundColor Green
Write-Host "NUNCA commite esses arquivos. Ambos estao no .gitignore." -ForegroundColor Cyan
Write-Host ""
Write-Host "Para o Render: copie o valor de HMAC_KEY e cole em Environment no dashboard." -ForegroundColor Cyan
