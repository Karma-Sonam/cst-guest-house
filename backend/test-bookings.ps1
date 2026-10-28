param([string]$BaseUrl = 'http://localhost:8080')
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
if (-not $settings.ADMIN_EMAIL -or -not $settings.ADMIN_PASSWORD) { throw 'Configure a local administrator before running booking checks.' }
$admin=(Call-Api POST '/api/auth/login' @{email=$settings.ADMIN_EMAIL;password=$settings.ADMIN_PASSWORD}).token
$password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$email="booking-$([Guid]::NewGuid().ToString('N'))@example.test"
$null=Call-Api POST '/api/auth/register' @{fullName='Booking Test';email=$email;password=$password} '' 201
$user=(Call-Api POST '/api/auth/login' @{email=$email;password=$password}).token
$otherEmail="other-$([Guid]::NewGuid().ToString('N'))@example.test"
$null=Call-Api POST '/api/auth/register' @{fullName='Other Test';email=$otherEmail;password=$password} '' 201
$other=(Call-Api POST '/api/auth/login' @{email=$otherEmail;password=$password}).token
$today=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow,'Asia/Thimphu').ToString('yyyy-MM-dd')
$tomorrow=([DateTime]::Parse($today)).AddDays(1).ToString('yyyy-MM-dd')
$future=([DateTime]::Parse($today)).AddDays(30).ToString('yyyy-MM-dd')
$departure=([DateTime]::Parse($today)).AddDays(32).ToString('yyyy-MM-dd')
function Booking-Body([int]$Guests,[bool]$Bed,[string]$Arrival,[string]$Departure) {
    @{fullName='Booking Test';email=$email;guestCategory='GENERAL_GUEST';purpose='Local integration test';guests=$Guests;checkIn=$Arrival;checkOut=$Departure;preferredCategory='STANDARD';extraBedRequested=$Bed}
}
$rooms=Call-Api GET '/api/rooms' $null $user
if ($rooms.Count -ne 10) { throw 'Expected ten rooms' }
$null=Call-Api GET "/api/rooms/available?checkIn=$future&checkOut=$departure&guests=4" $null $user 400
$null=Call-Api POST '/api/bookings' (Booking-Body 3 $false $future $departure) $user 400
$room=($rooms | Where-Object roomNumber -eq '101').id
$a=Call-Api POST '/api/bookings' (Booking-Body 2 $false $future $departure) $user 201
$b=Call-Api POST '/api/bookings' (Booking-Body 2 $false $future $departure) $user 201
$null=Call-Api GET "/api/bookings/$($a.id)" $null $other 404
$null=Call-Api PATCH "/api/admin/bookings/$($a.id)/approve" @{roomId=$room} $user 403
# Simultaneous admin requests must produce exactly one approval and one conflict.
$jobs=@()
foreach ($bookingId in @($a.id,$b.id)) {
    $jobs+=Start-Job -ArgumentList $BaseUrl,$bookingId,$room,$admin -ScriptBlock {
        param($url,$id,$roomId,$token)
        $response=Invoke-WebRequest -Uri "$url/api/admin/bookings/$id/approve" -Method Patch -ContentType 'application/json' -Body (@{roomId=$roomId;approveExtraBed=$false} | ConvertTo-Json) -Headers @{Authorization="Bearer $token"} -SkipHttpErrorCheck
        [int]$response.StatusCode
    }
}
try {
    $statuses=@($jobs | Wait-Job | Receive-Job | Sort-Object)
    if (($statuses -join ',') -ne '200,409') { throw "Concurrent approvals returned $($statuses -join ',')" }
    Write-Host 'PASS: Simultaneous approvals returned 200 and 409'
} finally { $jobs | Remove-Job -Force }
$available=Call-Api GET "/api/rooms/available?checkIn=$future&checkOut=$departure&guests=2" $null $user
if (@($available | Where-Object id -eq $room).Count -ne 0) { throw 'Reserved room appears available' }
$adjacent=Call-Api GET "/api/rooms/available?checkIn=$departure&checkOut=$(([DateTime]::Parse($departure)).AddDays(1).ToString('yyyy-MM-dd'))&guests=2" $null $user
if (@($adjacent | Where-Object id -eq $room).Count -ne 1) { throw 'Adjacent stay incorrectly blocked' }
foreach ($bookingId in @($a.id,$b.id)) { $null=Call-Api PATCH "/api/bookings/$bookingId/cancel" $null $admin }
$room2=($rooms | Where-Object roomNumber -eq '102').id
$stay=Call-Api POST '/api/bookings' (Booking-Body 3 $true $today $tomorrow) $user 201
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/approve" @{roomId=$room2;approveExtraBed=$false} $admin 409
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/approve" @{roomId=$room2;approveExtraBed=$true} $admin
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/extra-bed" @{approved=$false;remark='Cannot reduce capacity for three guests'} $admin 409
$null=Call-Api PATCH "/api/admin/rooms/$room2/status" @{status='MAINTENANCE'} $admin 409
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/check-in" $null $admin 409
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/extra-bed/setup" @{added=$true} $admin
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/check-in" $null $admin
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/check-out" $null $admin
$detail=Call-Api GET "/api/bookings/$($stay.id)" $null $user
if ($detail.booking.status -ne 'COMPLETED' -or @($detail.timeline | Where-Object newStatus -eq 'CHECKED_OUT').Count -ne 1) { throw 'Completion timeline incorrect' }
$released=Call-Api GET "/api/rooms/available?checkIn=$today&checkOut=$tomorrow&guests=2" $null $user
if (@($released | Where-Object id -eq $room2).Count -ne 1) { throw 'Checkout did not release room' }
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/check-out" $null $admin 409
$null=Call-Api PATCH "/api/admin/bookings/$($stay.id)/reopen" @{remark='Completed stays must remain immutable'} $admin 409
$reopened=Call-Api PATCH "/api/admin/bookings/$($a.id)/reopen" @{remark='Reconsider cancelled request'} $admin
if ($reopened.assignedRoomId -or $reopened.status -ne 'PENDING') { throw 'Reopening retained an allocation' }
$null=Call-Api PATCH "/api/admin/bookings/$($a.id)/remarks" @{remark='Reviewed by local administrator'} $admin
$null=Call-Api PATCH "/api/admin/bookings/$($a.id)/reject" @{remark='Integration test rejection'} $admin
$null=Call-Api GET '/api/bookings/my?page=0&size=10' $null $user
$null=Call-Api GET '/api/admin/bookings?status=PENDING&page=0&size=10' $null $admin
$room3=($rooms | Where-Object roomNumber -eq '103').id
$null=Call-Api PATCH "/api/admin/rooms/$room3/status" @{status='MAINTENANCE'} $admin
try {
    $maintenanceRequest=Call-Api POST '/api/bookings' (Booking-Body 2 $false $future $departure) $user 201
    $null=Call-Api PATCH "/api/admin/bookings/$($maintenanceRequest.id)/approve" @{roomId=$room3;approveExtraBed=$false} $admin 409
    $null=Call-Api PATCH "/api/bookings/$($maintenanceRequest.id)/cancel" $null $user
} finally { $null=Call-Api PATCH "/api/admin/rooms/$room3/status" @{status='ACTIVE'} $admin }
$special=($rooms | Where-Object roomNumber -eq '201').id
$vipBody=Booking-Body 1 $false $future $departure
$vipBody.preferredCategory='SPECIAL'; $vipBody.guestCategory='VICE_CHANCELLOR'
$vip=Call-Api POST '/api/bookings' $vipBody $user 201
$null=Call-Api PATCH "/api/admin/bookings/$($vip.id)/approve" @{roomId=$special;approveExtraBed=$false} $admin 409
# Synthetic capacity is used only for this local test and cleared afterwards.
$null=Call-Api PUT "/api/admin/rooms/$special/capacity" @{normalCapacity=2;maxCapacity=2} $user 403
$null=Call-Api PUT "/api/admin/rooms/$special/capacity" @{normalCapacity=2;maxCapacity=2} $admin
try {
    $null=Call-Api PATCH "/api/admin/bookings/$($vip.id)/approve" @{roomId=$special;approveExtraBed=$false;remark='Explicit VIP assignment for integration test'} $admin
    $null=Call-Api PUT "/api/admin/rooms/$special/capacity" @{normalCapacity=1;maxCapacity=1} $admin 409
    $null=Call-Api PATCH "/api/bookings/$($vip.id)/cancel" $null $admin
} finally { $null=Call-Api PUT "/api/admin/rooms/$special/capacity" @{normalCapacity=$null;maxCapacity=$null} $admin }
Write-Host 'Booking smoke checks passed. Test accounts and audited test stays remain in the local database.'
