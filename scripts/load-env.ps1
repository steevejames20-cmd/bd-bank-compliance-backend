# Charge les variables du fichier .env dans la session PowerShell courante,
# pour que 'mvn spring-boot:run' / 'mvn test' les voient.
# Équivalent Windows de : set -a; source .env; set +a (bash)
#
# Usage (depuis la racine du projet) :
#   .\scripts\load-env.ps1

$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-Host "Fichier .env introuvable. Copie d'abord .env.example en .env." -ForegroundColor Red
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#\s][^=]*)\s*=\s*(.*)\s*$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
        Write-Host "  $name chargée"
    }
}

Write-Host "Variables d'environnement chargées depuis .env" -ForegroundColor Green
