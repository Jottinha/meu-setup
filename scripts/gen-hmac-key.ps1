# Gera a chave HMAC compartilhada entre o backend (Spring) e o base.exe (Go).
# Idempotente: se hmac.key ja existe na raiz, nao faz nada.
# Saida: 64 caracteres hex (32 bytes) em hmac.key, UTF-8 sem BOM, sem newline.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$keyPath = Join-Path $projectRoot "hmac.key"

if (Test-Path $keyPath) {
    Write-Host "hmac.key ja existe em $keyPath (nao sobrescrito)." -ForegroundColor Yellow
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

Write-Host "hmac.key gerado em $keyPath (64 chars hex)." -ForegroundColor Green
Write-Host "NUNCA commite esse arquivo. Ele esta no .gitignore." -ForegroundColor Cyan
