$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ProgressPreference = "SilentlyContinue"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendLoginUri = "http://localhost:8080/api/system/login"
$offlineImagePackage = Join-Path $projectDir "animal-2.0-images.tar"
$logPath = Join-Path $projectDir "start-offline.log"

$requiredImages = @(
    "mysql:8.0",
    "redis:7-alpine",
    "elasticsearch:8.11.0",
    "livestock-health-backend:2.0",
    "livestock-health-frontend:2.0"
)

function Test-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-DockerReady {
    try {
        docker info *> $null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Test-PortListening {
    param([Parameter(Mandatory = $true)][int]$Port)
    try {
        return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    } catch {
        return $false
    }
}

function Test-FrontendContainerUsesPort {
    param([Parameter(Mandatory = $true)][int]$Port)
    try {
        $ports = docker ps --filter "name=livestock-frontend" --format "{{.Ports}}" 2>$null
        return [bool]($ports | Select-String -Pattern ":$Port->80/tcp" -Quiet)
    } catch {
        return $false
    }
}

function Get-FrontendHostPort {
    if ($env:FRONTEND_HOST_PORT) {
        return [int]$env:FRONTEND_HOST_PORT
    }

    if ((-not (Test-PortListening -Port 80)) -or (Test-FrontendContainerUsesPort -Port 80)) {
        return 80
    }

    Write-Host "[INFO] Port 80 is already used by another local service. Falling back to port 8088." -ForegroundColor Yellow
    foreach ($candidate in @(8088, 8081, 8082, 8083)) {
        if (-not (Test-PortListening -Port $candidate)) {
            return $candidate
        }
    }

    throw "No available frontend port found. Please free port 80 or 8088."
}

function Wait-DockerReady {
    param([int]$TimeoutSec = 180)

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-DockerReady) {
            return $true
        }
        Start-Sleep -Seconds 5
        Write-Host "  Waiting for Docker Desktop..." -ForegroundColor Gray
    }
    return $false
}

function Wait-HttpResponse {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [int]$TimeoutSec = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 10
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $response.StatusCode
            }
        } catch {}
        Start-Sleep -Seconds 5
    }

    return $null
}

function Wait-FrontendReady {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [int]$TimeoutSec = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 10
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500 -and $response.Content -match "畜牧健康|id=`"app`"") {
                return $response.StatusCode
            }
        } catch {}
        Start-Sleep -Seconds 5
    }

    return $null
}

function Wait-BackendLoginReady {
    param([int]$TimeoutSec = 180)

    $body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
    $deadline = (Get-Date).AddSeconds($TimeoutSec)

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Method Post -Uri $backendLoginUri -ContentType "application/json" -Body $body -TimeoutSec 10
            if ($response.success -eq $true) {
                return $true
            }
        } catch {}
        Start-Sleep -Seconds 5
    }

    return $false
}

function Test-DockerImageExists {
    param([Parameter(Mandatory = $true)][string]$Image)

    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        docker image inspect $Image 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Get-MissingImages {
    $missingImages = @()
    foreach ($image in $requiredImages) {
        if (Test-DockerImageExists -Image $image) {
            Write-Host "  [OK] $image" -ForegroundColor Green
        } else {
            Write-Host "  [MISSING] $image" -ForegroundColor Yellow
            $missingImages += $image
        }
    }

    return $missingImages
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Livestock Health Platform - Offline Startup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -LiteralPath $projectDir

try {
    Start-Transcript -Path $logPath -Force | Out-Null
    Write-Host "[INFO] Log file: $logPath" -ForegroundColor Gray
} catch {}

Write-Host "[1/4] Checking Docker..." -ForegroundColor Yellow
if (-not (Test-CommandAvailable "docker")) {
    Write-Host "[ERROR] Docker was not found. Please install and open Docker Desktop first." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Test-DockerReady)) {
    Write-Host "[INFO] Docker is not running. Trying to open Docker Desktop..." -ForegroundColor Yellow
    $dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (Test-Path -LiteralPath $dockerDesktop) {
        Start-Process -FilePath $dockerDesktop
    } else {
        Start-Process "Docker Desktop" -ErrorAction SilentlyContinue
    }

    if (-not (Wait-DockerReady -TimeoutSec 180)) {
        Write-Host "[ERROR] Docker Desktop did not become ready in time." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Host "[OK] Docker is ready" -ForegroundColor Green

$frontendHostPort = Get-FrontendHostPort
$env:FRONTEND_HOST_PORT = [string]$frontendHostPort
$frontendUri = if ($frontendHostPort -eq 80) { "http://localhost" } else { "http://localhost:$frontendHostPort" }
$loginUri = "$frontendUri/login"

Write-Host "[OK] Frontend host port: $frontendHostPort" -ForegroundColor Green

Write-Host ""
Write-Host "[2/4] Checking local images..." -ForegroundColor Yellow
$missing = Get-MissingImages

if ($missing.Count -gt 0) {
    if (Test-Path -LiteralPath $offlineImagePackage) {
        Write-Host ""
        Write-Host "[INFO] Missing images found, loading offline package:" -ForegroundColor Yellow
        Write-Host "  $offlineImagePackage" -ForegroundColor Gray
        docker load -i $offlineImagePackage
        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "[ERROR] docker load failed. The tar file may be incomplete or corrupted." -ForegroundColor Red
            Read-Host "Press Enter to exit"
            exit 1
        }

        Write-Host ""
        Write-Host "[INFO] Rechecking local images..." -ForegroundColor Yellow
        $missing = Get-MissingImages
    }
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERROR] These images are missing locally:" -ForegroundColor Red
    foreach ($image in $missing) {
        Write-Host "  - $image" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Put animal-2.0-images.tar in this project folder, or load it manually:" -ForegroundColor Gray
    Write-Host "  docker load -i .\animal-2.0-images.tar" -ForegroundColor Gray
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[3/4] Starting containers without pulling or building..." -ForegroundColor Yellow
docker compose up -d --no-build --pull never
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] docker compose failed." -ForegroundColor Red
    Write-Host "Run this for details:" -ForegroundColor Gray
    Write-Host "docker compose logs --tail=120" -ForegroundColor Gray
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[4/4] Waiting for services..." -ForegroundColor Yellow
docker compose ps
Write-Host ""

$frontendStatus = Wait-FrontendReady -Uri $loginUri -TimeoutSec 180
if ($frontendStatus) {
    Write-Host "[OK] Frontend responded: $loginUri (HTTP $frontendStatus)" -ForegroundColor Green
} else {
    Write-Host "[INFO] Frontend may still be starting. Refresh the browser later." -ForegroundColor Yellow
}

if (Wait-BackendLoginReady -TimeoutSec 180) {
    Write-Host "[OK] Backend login API is ready" -ForegroundColor Green
} else {
    Write-Host "[INFO] Backend may still be starting. Recent backend logs:" -ForegroundColor Yellow
    docker compose logs --tail=80 backend
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Offline startup finished" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host "Login: $loginUri" -ForegroundColor White
Write-Host "User:  admin" -ForegroundColor White
Write-Host "Pass:  admin123" -ForegroundColor White
Write-Host ""

Start-Process $loginUri
try { Stop-Transcript | Out-Null } catch {}
Read-Host "Press Enter to exit"
