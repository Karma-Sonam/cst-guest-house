param([string]$BaseUrl='http://127.0.0.1:5173')
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$settings=@{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $entry=$_.Split('=',2); $settings[$entry[0]]=$entry[1] }
}
$page=Invoke-WebRequest $BaseUrl
if ($page.StatusCode -ne 200) { throw 'Local website is unavailable.' }
$config=Invoke-RestMethod "$BaseUrl/api/public/config"
$login=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType application/json -Body (@{email=$settings.ADMIN_EMAIL;password=$settings.ADMIN_PASSWORD} | ConvertTo-Json)
$headers=@{Authorization="Bearer $($login.token)"}
try {
    $rooms=@(Invoke-RestMethod "$BaseUrl/api/admin/rooms" -Headers $headers)
    # Invoke-RestMethod can return the collection as a single array object.
    $rooms=@($rooms | ForEach-Object { $_ })
    $standard=@($rooms | Where-Object category -eq 'STANDARD')
    $special=@($rooms | Where-Object category -eq 'SPECIAL')
    if ($standard.Count -ne 8 -or $special.Count -ne 2) { throw 'Expected eight standard and two special rooms.' }
    foreach ($room in $standard) {
        if ($room.beds -ne 2 -or $room.normalCapacity -ne 2 -or $room.maxCapacity -ne 3 -or -not $room.supportsExtraBed) { throw "Standard room $($room.roomNumber) differs from the configured capacity rules." }
    }
    $anonymous=Invoke-WebRequest "$BaseUrl/api/admin/users" -SkipHttpErrorCheck
    if ([int]$anonymous.StatusCode -ne 401) { throw 'Administrator routes do not reject anonymous access.' }
    $snapshot=[ordered]@{
        checkedAtUtc=[DateTime]::UtcNow.ToString('o'); baseUrl=$BaseUrl; engineeringChecksPassed=$true
        scope='Website response, administrator access, inventory and standard-room rules only; not college acceptance or a full regression run.'
        registrationEnabled=$config.registrationEnabled
        inventory=@{standardRooms=$standard.Count;specialRooms=$special.Count}
        specialRooms=@($special | ForEach-Object { @{roomNumber=$_.roomNumber;beds=$_.beds;normalCapacity=$_.normalCapacity;maxCapacity=$_.maxCapacity} })
        missingContent=@{locations=@($rooms | Where-Object { -not $_.location } | ForEach-Object roomNumber); descriptions=@($rooms | Where-Object { -not $_.description } | ForEach-Object roomNumber); amenities=@($rooms | Where-Object { -not $_.amenities } | ForEach-Object roomNumber)}
        collegeAcceptance='NOT_RECORDED'
    }
    $logDirectory=Join-Path $projectRoot '.logs'
    $null=New-Item -ItemType Directory -Path $logDirectory -Force
    $snapshot | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $logDirectory 'local-readiness.json') -Encoding utf8
    Write-Host 'PASS: website, administrator access, inventory and standard-room rule checks.'
    Write-Host "Configuration snapshot: $logDirectory\local-readiness.json"
    Write-Host 'College confirmation and acceptance are recorded separately; missing content is not filled automatically.'
} finally {
    $null=Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/auth/logout" -Headers $headers
}
