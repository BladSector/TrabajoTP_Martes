@echo off
setlocal
set "CAFE_JAVA_DIR=%~dp0"
powershell.exe -NoProfile -Command "$ErrorActionPreference='Stop'; $jarCafe=Join-Path $env:CAFE_JAVA_DIR 'target\cafe-java-1.1.0.jar'; $procesosCafe=@(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -and $_.CommandLine.Contains($jarCafe) }); foreach ($procesoCafe in $procesosCafe) { Stop-Process -Id $procesoCafe.ProcessId; Write-Host ('Servidor Cafe Java detenido: ' + $procesoCafe.ProcessId) }; if ($procesosCafe.Count -eq 0) { Write-Host 'No hay servidores Cafe Java ejecutandose desde esta carpeta' }"
exit /b %errorlevel%
