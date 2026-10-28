param([string]$BaseUrl='http://127.0.0.1:5173')
$ErrorActionPreference='Stop'
function Call-Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='',[int]$Expected=200) {
    $options=@{Uri="$BaseUrl/api$Path";Method=$Method;SkipHttpErrorCheck=$true;Headers=@{Origin=$BaseUrl.TrimEnd('/')}}
    if ($Body) { $options.Body=$Body | ConvertTo-Json; $options.ContentType='application/json' }
    if ($Token) { $options.Headers.Authorization="Bearer $Token" }
    $result=Invoke-WebRequest @options
    if ([int]$result.StatusCode -ne $Expected) { throw "$Path expected $Expected, got $($result.StatusCode): $($result.Content)" }
    Write-Host "PASS: $Method $Path ($Expected)"
    if ($result.Content) { $result.Content | ConvertFrom-Json }
}
$config=Call-Api GET '/public/config'
if (-not $config.registrationEnabled) { throw 'Local web checks require registration to be enabled' }
$email="web-$([Guid]::NewGuid().ToString('N'))@example.test"
$password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$null=Call-Api POST '/auth/register' @{fullName='Web Integration';email=$email;password=$password} '' 201
$token=(Call-Api POST '/auth/login' @{email=$email;password=$password}).token
$profile=Call-Api PUT '/users/profile' @{fullName='Web Updated';phone='12345678';role='ADMIN';email='not-changed@example.test'} $token
if ($profile.fullName -ne 'Web Updated' -or $profile.role -ne 'USER' -or $profile.email -ne $email) { throw 'Profile update changed protected fields or failed' }
$null=Call-Api GET '/users/profile' $null $token
$null=Call-Api GET '/admin/dashboard/summary' $null $token 403
$day=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow,'Asia/Thimphu').Date
$booking=Call-Api POST '/bookings' @{fullName='Web Updated';email=$email;guestCategory='GENERAL_GUEST';purpose='Frontend proxy verification';guests=2;checkIn=$day.AddDays(60).ToString('yyyy-MM-dd');checkOut=$day.AddDays(61).ToString('yyyy-MM-dd');preferredCategory='STANDARD';extraBedRequested=$false} $token 201
$counts=Call-Api GET '/bookings/summary' $null $token
if ($counts.total -ne 1 -or $counts.pending -ne 1) { throw 'Guest dashboard totals are incorrect' }
$filtered=Call-Api GET '/bookings/my?status=CANCELLED' $null $token
if ($filtered.totalElements -ne 0) { throw 'Own booking status filter is incorrect' }
$null=Call-Api PATCH "/bookings/$($booking.id)/cancel" $null $token
$filtered=Call-Api GET '/bookings/my?status=CANCELLED' $null $token
if ($filtered.totalElements -ne 1) { throw 'Cancellation not reflected by own status filter' }
$settings=@{}
Get-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot -Parent) '.env') | ForEach-Object { if ($_ -match '^[A-Z][A-Z0-9_]*=') { $pair=$_.Split('=',2); $settings[$pair[0]]=$pair[1] } }
$admin=(Call-Api POST '/auth/login' @{email=$settings.ADMIN_EMAIL;password=$settings.ADMIN_PASSWORD}).token
$summary=Call-Api GET '/admin/dashboard/summary' $null $admin
if ($summary.totalRooms -ne 10 -or $summary.standardRooms -ne 8 -or $summary.specialRooms -ne 2) { throw 'Administrative inventory totals are incorrect' }
$null=Call-Api GET '/admin/current-guests' $null $admin
$null=Call-Api GET "/admin/arrivals-departures?date=$($day.ToString('yyyy-MM-dd'))&arrivals=true" $null $admin
$null=Call-Api GET "/admin/arrivals-departures?date=$($day.ToString('yyyy-MM-dd'))&arrivals=false" $null $admin
Write-Host 'Frontend proxy, profile, dashboard and date-query integration checks passed.'
