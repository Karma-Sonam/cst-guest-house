$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$logs=Join-Path $projectRoot '.logs'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
Push-Location $projectRoot
try {
    docker compose up -d --wait postgres
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not start. Open Docker Desktop and try again.' }
    function Is-Healthy([string]$Url) { try { $response=Invoke-WebRequest -Uri $Url -TimeoutSec 2; return $response.StatusCode -eq 200 } catch { return $false } }
    function Wait-Healthy([string]$Url) { for ($attempt=0;$attempt -lt 40;$attempt++) { if (Is-Healthy $Url) { return }; Start-Sleep -Seconds 1 }; throw "Server did not become ready: $Url. Check .logs." }
    if (-not (Is-Healthy 'http://127.0.0.1:8080/actuator/health')) {
        $backendScript=Join-Path $projectRoot 'backend\run-local.ps1'
        $process=Start-Process -FilePath (Get-Process -Id $PID).Path -ArgumentList @('-NoProfile','-File',"`"$backendScript`"") -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'backend.log') -RedirectStandardError (Join-Path $logs 'backend-error.log') -PassThru
        $process.Id | Set-Content -LiteralPath (Join-Path $logs 'backend.pid')
    }
    Wait-Healthy 'http://127.0.0.1:8080/actuator/health'
    if (-not (Is-Healthy 'http://127.0.0.1:5173/')) {
        $bundledNode='C:\Users\user\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
        $nodeExecutable=if ($env:NODE24_HOME) { Join-Path $env:NODE24_HOME 'node.exe' } elseif (Test-Path -LiteralPath $bundledNode) { $bundledNode } else { (Get-Command node.exe).Source }
        $version=& $nodeExecutable -p 'process.versions.node'
        if ([Version]$version -lt [Version]'24.15.0' -or [Version]$version -ge [Version]'25.0.0') { throw 'Use Node 24 LTS. Set NODE24_HOME to its installation directory.' }
        $vite=Join-Path $projectRoot 'frontend\node_modules\vite\bin\vite.js'
        if (-not (Test-Path -LiteralPath $vite)) { throw 'Install frontend dependencies first.' }
        $process=Start-Process -FilePath $nodeExecutable -ArgumentList @("`"$vite`"",'--host','127.0.0.1') -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'frontend.log') -RedirectStandardError (Join-Path $logs 'frontend-error.log') -PassThru
        $process.Id | Set-Content -LiteralPath (Join-Path $logs 'frontend.pid')
    }
    Wait-Healthy 'http://127.0.0.1:5173/api/public/config'
    Write-Output 'Website ready: http://127.0.0.1:5173/'
    Write-Output 'Backend healthy: http://127.0.0.1:8080/actuator/health'
    Write-Output 'Background server logs are in D:\GuestHouse\.logs.'
} finally { Pop-Location }
