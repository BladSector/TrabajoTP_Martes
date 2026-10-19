$ErrorActionPreference = "Stop"
$jarCafe = Join-Path $PSScriptRoot "target\cafe-java-1.1.0.jar"
$procesosCafe = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -and $_.CommandLine.Contains($jarCafe) }
foreach ($procesoCafe in $procesosCafe) {
    Stop-Process -Id $procesoCafe.ProcessId
    Write-Host "Servidor Cafe Java detenido: $($procesoCafe.ProcessId)"
}
if (-not $procesosCafe) { Write-Host "No hay servidores Cafe Java ejecutandose desde esta carpeta" }
