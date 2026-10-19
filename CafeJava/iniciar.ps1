param(
    [ValidateSet("mysql", "demo")][string]$Perfil = "mysql",
    [ValidateRange(1024, 65535)][int]$Puerto = 8081
)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
$claveTemporal = $false
$configuracionLocalCafe = Test-Path -LiteralPath (Join-Path $PSScriptRoot 'application-local.properties')
try {
    if ($Perfil -eq "mysql") {
        if (-not $env:CAFE_DB_USER -and -not $configuracionLocalCafe) {
            $usuarioCafe = Read-Host "Usuario MySQL (por defecto cafe_java)"
            $env:CAFE_DB_USER = if ($usuarioCafe) { $usuarioCafe } else { "cafe_java" }
        }
        if (-not $env:CAFE_DB_PASSWORD -and -not $configuracionLocalCafe) {
            $claveCafe = Read-Host "Clave MySQL" -AsSecureString
            $env:CAFE_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $claveCafe).Password
            $claveTemporal = $true
        }
    }
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar el proyecto" }
    Write-Host "Cafe Java: http://localhost:$Puerto - Perfil: $Perfil"
    & java -jar "$PSScriptRoot\target\cafe-java-1.1.0.jar" "--spring.profiles.active=$Perfil" "--server.port=$Puerto"
} finally {
    if ($claveTemporal) { Remove-Item Env:CAFE_DB_PASSWORD -ErrorAction SilentlyContinue }
}
