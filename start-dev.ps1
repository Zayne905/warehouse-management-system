param(
    [switch]$NoStop
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $Root "warehouse-backend"
$FrontendDir = Join-Path $Root "warehouse-frontend"
$BackendJar = Join-Path $BackendDir "target\warehouse-backend-1.0.0.jar"

function Stop-PortProcess {
    param([int]$Port)

    $pids = netstat -ano |
        Select-String "LISTENING" |
        Select-String ":$Port" |
        ForEach-Object { ($_ -split "\s+")[-1] } |
        Select-Object -Unique

    foreach ($pid in $pids) {
        if ($pid -and $pid -ne "0") {
            Write-Host "Stopping port $Port process PID=$pid ..."
            Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        }
    }
}

function Assert-File {
    param(
        [string]$Path,
        [string]$Message
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Message`nMissing: $Path"
    }
}

Write-Host "Warehouse Management System dev launcher"
Write-Host "Root: $Root"

Assert-File $BackendJar "Backend jar not found. Build backend first with: mvn -DskipTests package"
Assert-File (Join-Path $FrontendDir "package.json") "Frontend package.json not found."

if (-not $NoStop) {
    Stop-PortProcess 5173
    Stop-PortProcess 8081
    Start-Sleep -Seconds 2
}

Write-Host "Starting backend on http://127.0.0.1:8081 ..."
Start-Process powershell -WorkingDirectory $BackendDir -ArgumentList @(
    "-NoExit",
    "-Command",
    "java -jar target\warehouse-backend-1.0.0.jar"
)

Write-Host "Starting frontend on http://127.0.0.1:5173 ..."
Start-Process powershell -WorkingDirectory $FrontendDir -ArgumentList @(
    "-NoExit",
    "-Command",
    "npm.cmd run dev -- --host 0.0.0.0"
)

Write-Host "Waiting for services ..."
Start-Sleep -Seconds 8

Write-Host ""
Write-Host "Port status:"
netstat -ano | findstr "5173 8081"

Write-Host ""
Write-Host "Open:"
Write-Host "  http://127.0.0.1:5173"
Write-Host ""
Write-Host "Tip: if the page keeps showing request failed, clear browser token:"
Write-Host "  localStorage.clear(); location.href='/login'"
