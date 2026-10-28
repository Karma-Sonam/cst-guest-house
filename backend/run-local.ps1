param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$environmentFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Create the root .env from .env.example first.' }
foreach ($line in Get-Content -LiteralPath $environmentFile) {
    if ($line -match '^\s*#' -or [string]::IsNullOrWhiteSpace($line)) { continue }
    $entry = $line.Split('=', 2)
    if ($entry.Length -ne 2 -or $entry[0] -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid environment entry.' }
    [Environment]::SetEnvironmentVariable($entry[0], $entry[1], 'Process')
}
$javaExecutable = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'C:\Program Files\Java\jdk-21\bin\java.exe' }
if (-not (Test-Path -LiteralPath $javaExecutable)) { throw 'Set JAVA_HOME to your Java 21 installation.' }
$jar = Join-Path $PSScriptRoot 'target\guesthouse-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build first with .\mvnw.cmd package.' }
& $javaExecutable -jar $jar "--server.port=$Port"
exit $LASTEXITCODE
