$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$settings=@{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $entry=$_.Split('=',2); $settings[$entry[0]]=$entry[1] }
}
$databaseUser=if ($settings.POSTGRES_USER) { $settings.POSTGRES_USER } else { 'guesthouse' }
$databasePort=if ($settings.POSTGRES_PORT) { $settings.POSTGRES_PORT } else { '55432' }
Push-Location $projectRoot
try {
    docker compose up -d --wait postgres
    if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop before running PostgreSQL tests.' }
    $exists=docker compose exec -T postgres psql -U $databaseUser -d postgres -tAc "SELECT count(*) FROM pg_database WHERE datname='guesthouse_test'"
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect test database.' }
    if ([int](($exists -join '').Trim()) -eq 0) {
        docker compose exec -T postgres createdb -U $databaseUser guesthouse_test
        if ($LASTEXITCODE -ne 0) { throw 'Could not create guesthouse_test.' }
    }
    $env:RUN_POSTGRES_TESTS='true'
    $env:TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:$databasePort/guesthouse_test"
    $env:TEST_DATABASE_USERNAME=$databaseUser
    $env:TEST_DATABASE_PASSWORD=$settings.POSTGRES_PASSWORD
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME='C:\Program Files\Java\jdk-21' }
    $env:Path="$env:JAVA_HOME\bin;$env:Path"
    Set-Location $PSScriptRoot
    & .\mvnw.cmd -B -ntp test
    $testExit=$LASTEXITCODE
} finally {
    Remove-Item Env:RUN_POSTGRES_TESTS,Env:TEST_DATABASE_URL,Env:TEST_DATABASE_USERNAME,Env:TEST_DATABASE_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
exit $testExit
