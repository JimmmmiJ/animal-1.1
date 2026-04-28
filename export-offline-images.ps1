$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ProgressPreference = "SilentlyContinue"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputDir = Join-Path $projectDir "offline-images"
$outputTar = Join-Path $outputDir "animal-2.0-images.tar"

$runtimeImages = @(
    "mysql:8.0",
    "redis:7-alpine",
    "elasticsearch:8.11.0"
)

$appImages = @(
    "livestock-health-backend:2.0",
    "livestock-health-frontend:2.0"
)

function Test-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

Set-Location -LiteralPath $projectDir

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Livestock Health Platform - Export Images" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-CommandAvailable "docker")) {
    Write-Host "[ERROR] Docker was not found. Please install and open Docker Desktop first." -ForegroundColor Red
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker Desktop is not ready. Open Docker Desktop and try again." -ForegroundColor Red
    exit 1
}

Write-Host "[1/4] Pulling runtime images..." -ForegroundColor Yellow
foreach ($image in $runtimeImages) {
    Write-Host "  docker pull $image" -ForegroundColor Gray
    docker pull $image
}

Write-Host ""
Write-Host "[2/4] Building application images..." -ForegroundColor Yellow
docker compose build backend frontend

Write-Host ""
Write-Host "[3/4] Checking required images..." -ForegroundColor Yellow
$allImages = $runtimeImages + $appImages
foreach ($image in $allImages) {
    docker image inspect $image *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Required image is missing after build: $image"
    }
    Write-Host "  [OK] $image" -ForegroundColor Green
}

Write-Host ""
Write-Host "[4/4] Saving images to tar package..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
if (Test-Path -LiteralPath $outputTar) {
    Remove-Item -LiteralPath $outputTar -Force
}
docker save -o $outputTar @allImages

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Export finished" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host "Image package: $outputTar" -ForegroundColor White
Write-Host ""
Write-Host "Send this tar file to the offline computer, then run:" -ForegroundColor Gray
Write-Host "  docker load -i animal-2.0-images.tar" -ForegroundColor Gray
Write-Host "  .\start-offline.ps1" -ForegroundColor Gray
