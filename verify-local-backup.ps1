param([Parameter(Mandatory=$true)][string]$BackupPath)
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$resolved=(Resolve-Path -LiteralPath $BackupPath).Path
$manifest=Get-Content -LiteralPath "$resolved.json" -Raw | ConvertFrom-Json
if ($manifest.format -ne 'postgresql-custom' -or (Get-Item -LiteralPath $resolved).Length -ne $manifest.bytes -or
    (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash -ne $manifest.sha256) { throw 'Backup integrity check failed. No database was created.' }
$settings=@{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $entry=$_.Split('=',2); $settings[$entry[0]]=$entry[1] }
}
$databaseUser=if ($settings.POSTGRES_USER) { $settings.POSTGRES_USER } else { 'guesthouse' }
if ($databaseUser -notmatch '^[A-Za-z_][A-Za-z0-9_]{0,62}$') { throw 'Unsupported database user identifier.' }
$suffix=[Guid]::NewGuid().ToString('N')
$restoreDatabase="guesthouse_restore_${suffix}_test"
$containerPath="/tmp/guesthouse-restore-$suffix.dump"
$created=$false; $verified=$false; $cleanupOk=$true
Push-Location $projectRoot
try {
    docker compose -f compose.yaml cp $resolved "postgres:$containerPath"
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy backup into the local PostgreSQL container.' }
    docker compose -f compose.yaml exec -T postgres createdb -U $databaseUser --template=template0 $restoreDatabase
    if ($LASTEXITCODE -ne 0) { throw 'Could not create a new disposable restore database.' }
    $created=$true
    docker compose -f compose.yaml exec -T postgres pg_restore -U $databaseUser --dbname=$restoreDatabase --no-owner --no-privileges --single-transaction --exit-on-error $containerPath
    if ($LASTEXITCODE -ne 0) { throw 'Restore failed. The working database was not modified.' }
    $sql="SELECT json_build_object('migrations',(SELECT count(*) FROM flyway_schema_history WHERE success),'rooms',(SELECT count(*) FROM rooms),'users',(SELECT count(*) FROM app_users),'bookings',(SELECT count(*) FROM bookings),'facilityRequests',(SELECT count(*) FROM facility_requests),'constraints',(SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace));"
    $output=docker compose -f compose.yaml exec -T postgres psql -U $databaseUser -d $restoreDatabase -v ON_ERROR_STOP=1 -tAc $sql
    if ($LASTEXITCODE -ne 0) { throw 'Restored schema checks failed.' }
    $counts=($output -join '') | ConvertFrom-Json
    if ($counts.migrations -lt 5 -or $counts.rooms -ne 10 -or $counts.constraints -lt 1) { throw 'Restored data does not match the expected guest-house schema/inventory.' }
    $report=[ordered]@{ verifiedAtUtc=[DateTime]::UtcNow.ToString('o'); sha256=$manifest.sha256; restored=$counts; scope='Restored into a new disposable database; no working database was replaced.' }
    $verified=$true
} finally {
    if ($created) {
        # Only the uniquely named database created by this invocation can be removed.
        if ($restoreDatabase -notmatch '^guesthouse_restore_[a-f0-9]{32}_test$') { throw 'Refusing cleanup of an unexpected database name.' }
        docker compose -f compose.yaml exec -T postgres dropdb -U $databaseUser $restoreDatabase
        if ($LASTEXITCODE -ne 0) { $cleanupOk=$false; Write-Warning "Remove the disposable database manually after investigation: $restoreDatabase" }
    }
    docker compose -f compose.yaml exec -T postgres rm -f -- $containerPath
    if ($LASTEXITCODE -ne 0) { $cleanupOk=$false; Write-Warning "Temporary restore file remains in the container: $containerPath" }
    Pop-Location
}
if (-not $cleanupOk) { throw 'Restore verification cleanup failed; inspect the warnings.' }
if ($verified) {
    $report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$resolved.verified.json" -Encoding utf8
    Write-Host 'PASS: backup hash, isolated PostgreSQL restore, schema and inventory checks; disposable database removed.'
    Write-Host "Verification report: $resolved.verified.json"
}
