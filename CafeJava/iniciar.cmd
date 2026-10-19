@echo off
setlocal
cd /d "%~dp0"

set "CAFE_JAVA_PERFIL=%~1"
if not defined CAFE_JAVA_PERFIL set "CAFE_JAVA_PERFIL=mysql"
if /I "%CAFE_JAVA_PERFIL%"=="mysql" goto perfil_valido
if /I "%CAFE_JAVA_PERFIL%"=="demo" goto perfil_valido
echo Perfil invalido. Use mysql o demo.
exit /b 1

:perfil_valido
set "CAFE_JAVA_PUERTO=%~2"
if not defined CAFE_JAVA_PUERTO set "CAFE_JAVA_PUERTO=8081"
powershell.exe -NoProfile -Command "$puertoCafe=0; if (-not [int]::TryParse($env:CAFE_JAVA_PUERTO,[ref]$puertoCafe) -or $puertoCafe -lt 1024 -or $puertoCafe -gt 65535) { exit 1 }"
if errorlevel 1 (
    echo Puerto invalido. Use un numero entre 1024 y 65535.
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo No se encontro Java. Instale Java 17 y agreguelo al PATH.
    exit /b 1
)

call "%~dp0detener.cmd"
if errorlevel 1 exit /b 1

if exist "%~dp0target\cafe-java-1.1.0.jar" goto iniciar
call mvn -q -DskipTests package
if errorlevel 1 (
    echo No se pudo compilar el proyecto.
    exit /b 1
)

:iniciar
echo Cafe Java: http://localhost:%CAFE_JAVA_PUERTO% - Perfil: %CAFE_JAVA_PERFIL%
java -jar "%~dp0target\cafe-java-1.1.0.jar" "--spring.profiles.active=%CAFE_JAVA_PERFIL%" "--server.port=%CAFE_JAVA_PUERTO%"
exit /b %errorlevel%
