param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
function Assert-Status($Response, [int]$Expected, [string]$Description) {
    if ([int]$Response.StatusCode -ne $Expected) { throw "$Description expected $Expected, got $($Response.StatusCode)" }
    Write-Output "PASS: $Description ($Expected)"
}
function Request([string]$Method, [string]$Path, $Body = $null, [string]$Token = '') {
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; SkipHttpErrorCheck = $true }
    if ($Body) { $parameters.Body = ($Body | ConvertTo-Json); $parameters.ContentType = 'application/json' }
    if ($Token) { $parameters.Headers = @{ Authorization = "Bearer $Token" } }
    Invoke-WebRequest @parameters
}
$email = "smoke-$([Guid]::NewGuid().ToString('N'))@example.test"
$password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
Assert-Status (Request GET '/actuator/health') 200 'Application and database health'
Assert-Status (Request GET '/api/auth/me') 401 'Anonymous profile access denied'
Assert-Status (Request POST '/api/auth/register' @{ fullName='API Smoke Test'; email=$email; password='short' }) 400 'Weak password rejected'
$registration = Request POST '/api/auth/register' @{ fullName='API Smoke Test'; email=$email; password=$password; role='ADMIN' }
Assert-Status $registration 201 'Registration'
if (($registration.Content | ConvertFrom-Json).role -ne 'USER') { throw 'Registration allowed role escalation' }
Write-Output 'PASS: Registration cannot choose ADMIN'
Assert-Status (Request POST '/api/auth/register' @{ fullName='Duplicate'; email=$email; password=$password }) 409 'Duplicate email rejected'
Assert-Status (Request POST '/api/auth/login' @{ email=$email; password='incorrect-password' }) 401 'Wrong credentials rejected'
$login = Request POST '/api/auth/login' @{ email=$email; password=$password }
Assert-Status $login 200 'Login'
$token = ($login.Content | ConvertFrom-Json).token
$profile = Request GET '/api/auth/me' $null $token
Assert-Status $profile 200 'Authenticated profile'
if (($profile.Content | ConvertFrom-Json).email -ne $email) { throw 'Unexpected profile identity' }
Assert-Status (Request GET '/api/admin/bookings' $null $token) 403 'USER denied admin route'
Assert-Status (Request POST '/api/auth/logout' $null $token) 204 'Logout'
Assert-Status (Request GET '/api/auth/me' $null $token) 401 'Revoked token rejected'
Write-Output 'Authentication API smoke checks passed. A uniquely named test user remains in the local database.'
