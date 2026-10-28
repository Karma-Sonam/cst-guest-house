param([string]$BaseUrl='http://127.0.0.1:5173')
$ErrorActionPreference='Stop'
$settings=@{}
Get-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot -Parent) '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $entry=$_.Split('=',2); $settings[$entry[0]]=$entry[1] }
}
$login=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType application/json -Body (@{email=$settings.ADMIN_EMAIL;password=$settings.ADMIN_PASSWORD} | ConvertTo-Json)
$headers=@{Authorization="Bearer $($login.token)"}
try {
    $users=Invoke-RestMethod "$BaseUrl/api/admin/users?search=$([Uri]::EscapeDataString($settings.ADMIN_EMAIL))&size=10" -Headers $headers
    if ($users.totalElements -ne 1 -or $users.content[0].id -ne $login.user.id -or ($users | ConvertTo-Json -Depth 10) -match 'passwordHash') { throw 'Safe administrator user search failed' }
    Write-Host 'PASS: safe user search through web proxy'
    $self=Invoke-WebRequest -Method Patch -Uri "$BaseUrl/api/admin/users/$($login.user.id)/active" -Headers $headers -ContentType application/json -Body '{"active":false}' -SkipHttpErrorCheck
    if ([int]$self.StatusCode -ne 409) { throw 'Self-deactivation protection failed' }
    Write-Host 'PASS: administrator self-deactivation rejected'
    $day=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow,'Asia/Thimphu').Date
    $from=$day.ToString('yyyy-MM-dd'); $to=$day.AddDays(1).ToString('yyyy-MM-dd')
    $report=Invoke-RestMethod "$BaseUrl/api/admin/reports/summary?from=$from&to=$to" -Headers $headers
    if ($report.days -ne 1 -or $report.reservedRoomNights -lt 0) { throw 'Report date calculation failed' }
    Write-Host 'PASS: report summary through web proxy'
    $rooms=Invoke-RestMethod "$BaseUrl/api/admin/rooms" -Headers $headers
    if ($rooms.Count -ne 10 -or 'description' -notin $rooms[0].PSObject.Properties.Name -or 'amenities' -notin $rooms[0].PSObject.Properties.Name) { throw 'Room details schema unavailable' }
    Write-Host 'PASS: extended room details and inventory'
    $bookings=Invoke-RestMethod "$BaseUrl/api/admin/bookings?size=1" -Headers $headers
    if ($bookings.content.Count) {
        $reference=[Uri]::EscapeDataString($bookings.content[0].reference.ToLowerInvariant())
        $matches=Invoke-RestMethod "$BaseUrl/api/admin/bookings?search=$reference" -Headers $headers
        if ($matches.totalElements -ne 1 -or $matches.content[0].id -ne $bookings.content[0].id) { throw 'Reference search failed' }
        Write-Host 'PASS: case-insensitive booking reference search'
    }
    $anonymous=Invoke-WebRequest "$BaseUrl/api/admin/users" -SkipHttpErrorCheck
    if ([int]$anonymous.StatusCode -ne 401) { throw 'Anonymous administrator access was not rejected' }
    Write-Host 'PASS: administrator routes require authentication'
} finally {
    $null=Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/auth/logout" -Headers $headers
}
