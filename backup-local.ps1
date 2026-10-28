param()
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$settings=@{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $entry=$_.Split('=',2); $settings[$entry[0]]=$entry[1] }
}
$databaseUser=if ($settings.POSTGRES_USER) { $settings.POSTGRES_USER } else { 'guesthouse' }
$databaseName=if ($settings.POSTGRES_DB) { $settings.POSTGRES_DB } else { 'guesthouse' }
foreach ($identifier in @($databaseUser,$databaseName)) { if ($identifier -notmatch '^[A-Za-z_][A-Za-z0-9_]{0,62}$') { throw 'Unsupported database/user identifier in .env.' } }
$backupRoot=Join-Path $projectRoot '.backups'
$null=New-Item -ItemType Directory -Path $backupRoot -Force
$stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$suffix=[Guid]::NewGuid().ToString('N')
$backupPath=Join-Path $backupRoot "$databaseName-$stamp-$suffix.dump"
$containerPath="/tmp/guesthouse-backup-$suffix.dump"
Push-Location $projectRoot
try {
    docker compose -f compose.yaml exec -T postgres pg_dump -U $databaseUser -d $databaseName --format=custom --file=$containerPath
    if ($LASTEXITCODE -ne 0) { throw 'Database backup failed; verify that the local PostgreSQL container is running.' }
    docker compose -f compose.yaml cp "postgres:$containerPath" $backupPath
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the backup to the local backup folder.' }
    $file=Get-Item -LiteralPath $backupPath
    if ($file.Length -lt 1) { throw 'The backup is empty.' }
    $manifest=[ordered]@{ format='postgresql-custom'; database=$databaseName; createdAtUtc=[DateTime]::UtcNow.ToString('o'); bytes=$file.Length; sha256=(Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash; postgresMajor=17 }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath "$backupPath.json" -Encoding utf8
    Write-Host "Backup saved: $backupPath"
    Write-Host 'Contains private guest/account data. Keep this folder private; the dump is not encrypted.'
} finally {
    docker compose -f compose.yaml exec -T postgres rm -f -- $containerPath
    if ($LASTEXITCODE -ne 0) { Write-Warning "Could not remove the temporary container file: $containerPath" }
    Pop-Location
}
