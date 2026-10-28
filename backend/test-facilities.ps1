param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
function Call-Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='',[int]$Expected=200) {
    $parameters=@{Uri="$BaseUrl$Path";Method=$Method;SkipHttpErrorCheck=$true}
    if ($Body) { $parameters.Body=($Body | ConvertTo-Json); $parameters.ContentType='application/json' }
    if ($Token) { $parameters.Headers=@{Authorization="Bearer $Token"} }
    $response=Invoke-WebRequest @parameters
    if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path expected $Expected, got $($response.StatusCode): $($response.Content)" }
    Write-Host "PASS: $Method $Path ($Expected)"
    if ($response.Content) { return ($response.Content | ConvertFrom-Json) }
}
$settings=@{}
Get-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot -Parent) '.env') | ForEach-Object {
    if ($_ -match '^[A-Z][A-Z0-9_]*=') { $pair=$_.Split('=',2); $settings[$pair[0]]=$pair[1] }
}
$admin=(Call-Api POST '/api/auth/login' @{email=$settings.ADMIN_EMAIL;password=$settings.ADMIN_PASSWORD}).token
$password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$email="facility-$([Guid]::NewGuid().ToString('N'))@example.test"
$null=Call-Api POST '/api/auth/register' @{fullName='Facility Test';email=$email;password=$password} '' 201
$user=(Call-Api POST '/api/auth/login' @{email=$email;password=$password}).token
$otherEmail="other-facility-$([Guid]::NewGuid().ToString('N'))@example.test"
$null=Call-Api POST '/api/auth/register' @{fullName='Other Facility Test';email=$otherEmail;password=$password} '' 201
$other=(Call-Api POST '/api/auth/login' @{email=$otherEmail;password=$password}).token
$today=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow,'Asia/Thimphu').Date
$date=$today.AddDays(40).ToString('yyyy-MM-dd')
$date2=$today.AddDays(41).ToString('yyyy-MM-dd')
$date3=$today.AddDays(42).ToString('yyyy-MM-dd')
$reason=@{remark='Local integration test decision'}
function Convention-Body([string]$Date,[string]$Start='10:00',[string]$End='12:00',$Booking=$null) {
    @{bookingId=$Booking;title='College event';purpose='Facility integration test';eventDate=$Date;startTime=$Start;endTime=$End;expectedParticipants=20}
}
function Dining-Body([string]$Date,[string]$Period='LUNCH',$Booking=$null) {
    @{bookingId=$Booking;title='Official guest dining hall usage';purpose='Schedule test only';usageDate=$Date;mealPeriod=$Period;numberOfGuests=10}
}
$null=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date '12:00' '10:00') $user 400
$c1=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date) $user 201
$c2=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date '11:00' '13:00') $user 201
$null=Call-Api GET "/api/facility-requests/$($c1.id)" $null $other 404
$null=Call-Api PATCH "/api/admin/convention-hall/$($c1.id)/approve" $reason $user 403
$null=Call-Api PATCH "/api/admin/convention-hall/$($c1.id)/approve" $reason $admin
$conflict=Call-Api GET "/api/facility-requests/$($c2.id)" $null $user
if (-not $conflict.request.schedulingConflict) { throw 'Pending overlap not flagged' }
$null=Call-Api PATCH "/api/admin/convention-hall/$($c2.id)/approve" $reason $admin 409
$adjacent=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date '12:00' '13:00') $user 201
$null=Call-Api PATCH "/api/admin/convention-hall/$($adjacent.id)/approve" $reason $admin
$null=Call-Api PATCH "/api/admin/dining-hall/$($c2.id)/reject" $reason $admin 404
$null=Call-Api PATCH "/api/admin/facility-requests/$($c1.id)/complete" $reason $admin 409
$null=Call-Api PATCH "/api/facility-requests/$($c1.id)/cancel" $reason $user 409
$null=Call-Api PATCH "/api/facility-requests/$($c1.id)/cancel" $reason $admin
$null=Call-Api PATCH "/api/admin/convention-hall/$($c1.id)/approve" $reason $admin 409
$null=Call-Api PATCH "/api/admin/convention-hall/$($c2.id)/reject" $reason $admin
$null=Call-Api PATCH "/api/facility-requests/$($adjacent.id)/cancel" $reason $admin
$parallel1=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date2) $user 201
$parallel2=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date2 '11:00' '13:00') $user 201
$jobs=@()
foreach ($id in @($parallel1.id,$parallel2.id)) {
    $jobs+=Start-Job -ArgumentList $BaseUrl,$id,$admin -ScriptBlock {
        param($url,$requestId,$token)
        $response=Invoke-WebRequest -Uri "$url/api/admin/convention-hall/$requestId/approve" -Method Patch -ContentType 'application/json' -Body '{"remark":"Concurrent integration test"}' -Headers @{Authorization="Bearer $token"} -SkipHttpErrorCheck
        [int]$response.StatusCode
    }
}
try {
    $statuses=@($jobs | Wait-Job | Receive-Job | Sort-Object)
    if (($statuses -join ',') -ne '200,409') { throw "Concurrent hall approvals returned $($statuses -join ',')" }
    Write-Host 'PASS: Simultaneous hall approvals returned 200 and 409'
} finally { $jobs | Remove-Job -Force }
foreach ($id in @($parallel1.id,$parallel2.id)) { $null=Call-Api PATCH "/api/facility-requests/$id/cancel" $reason $admin }
$periods=Call-Api GET '/api/dining-hall/periods' $null $user
$d1=Call-Api POST '/api/dining-hall/requests' (Dining-Body $date) $user 201
if ($d1.startTime -ne $periods.LUNCH[0]) { throw 'Dining period differs from configured window' }
$d2=Call-Api POST '/api/dining-hall/requests' (Dining-Body $date) $user 201
$null=Call-Api PATCH "/api/admin/dining-hall/$($d1.id)/approve" $reason $admin
$null=Call-Api PATCH "/api/admin/dining-hall/$($d2.id)/approve" $reason $admin 409
$custom=Dining-Body $date 'OTHER'; $custom.startTime=$periods.LUNCH[0]; $custom.endTime=$periods.LUNCH[1]
$d3=Call-Api POST '/api/dining-hall/requests' $custom $user 201
$null=Call-Api PATCH "/api/admin/dining-hall/$($d3.id)/approve" $reason $admin 409
$null=Call-Api POST '/api/dining-hall/requests' (Dining-Body $date 'OTHER') $user 400
$override=Dining-Body $date; $override.startTime='01:00'; $override.endTime='02:00'
$null=Call-Api POST '/api/dining-hall/requests' $override $user 400
foreach ($id in @($d1.id,$d2.id,$d3.id)) { $null=Call-Api PATCH "/api/facility-requests/$id/cancel" $reason $admin }
$accommodation=Call-Api POST '/api/bookings' @{fullName='Facility Test';email=$email;guestCategory='OFFICIAL_GUEST';purpose='Linked facility test';guests=2;checkIn=$date3;checkOut=$today.AddDays(44).ToString('yyyy-MM-dd');preferredCategory='STANDARD';extraBedRequested=$false} $user 201
$linkedConvention=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date3 '10:00' '12:00' $accommodation.id) $user 201
$linkedDining=Call-Api POST '/api/dining-hall/requests' (Dining-Body $date3 'DINNER' $accommodation.id) $user 201
$linkedApproved=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date3 '14:00' '15:00' $accommodation.id) $user 201
$null=Call-Api PATCH "/api/admin/convention-hall/$($linkedApproved.id)/approve" $reason $admin
$null=Call-Api POST '/api/convention-hall/requests' (Convention-Body $date3 '16:00' '17:00' $accommodation.id) $other 404
$null=Call-Api PATCH "/api/bookings/$($accommodation.id)/cancel" $null $user
foreach ($id in @($linkedConvention.id,$linkedDining.id)) {
    $detail=Call-Api GET "/api/facility-requests/$id" $null $user
    if ($detail.request.status -ne 'CANCELLED' -or $detail.timeline.Count -lt 2) { throw 'Linked pending request was not cancelled with history' }
}
$retained=Call-Api GET "/api/facility-requests/$($linkedApproved.id)" $null $user
if ($retained.request.status -ne 'APPROVED') { throw 'Approved linked reservation was silently changed' }
$null=Call-Api PATCH "/api/facility-requests/$($linkedApproved.id)/cancel" $reason $admin
$null=Call-Api POST '/api/dining-hall/requests' (Dining-Body $date3 'LUNCH' $accommodation.id) $user 409
$null=Call-Api GET '/api/convention-hall/my?page=0&size=10' $null $user
$null=Call-Api GET '/api/dining-hall/my?page=0&size=10' $null $user
$null=Call-Api GET "/api/admin/convention-hall/requests?date=$date&status=CANCELLED" $null $admin
$null=Call-Api GET '/api/admin/dining-hall/requests' $null $admin
$filtered=Call-Api GET "/api/admin/dining-hall/requests?date=$date&mealPeriod=LUNCH&status=CANCELLED" $null $admin
if (@($filtered.content | Where-Object { $_.mealPeriod -ne 'LUNCH' -or $_.usageDate -ne $date -or $_.status -ne 'CANCELLED' }).Count -gt 0) { throw 'Dining filters returned unrelated requests' }
Write-Host 'Facility smoke checks passed. Audited test requests remain in the local database.'
