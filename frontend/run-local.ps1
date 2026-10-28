param([ValidateSet('dev','build','test')][string]$Action='dev')
$ErrorActionPreference='Stop'
$bundledNode='C:\Users\user\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
$nodeExecutable=if ($env:NODE24_HOME) { Join-Path $env:NODE24_HOME 'node.exe' } elseif (Test-Path -LiteralPath $bundledNode) { $bundledNode } else { (Get-Command node.exe).Source }
$nodeVersion=& $nodeExecutable -p 'process.versions.node'
if ([Version]$nodeVersion -lt [Version]'24.15.0' -or [Version]$nodeVersion -ge [Version]'25.0.0') { throw 'Use Node 24 LTS (24.15 or newer). Set NODE24_HOME to its folder if necessary.' }
Push-Location $PSScriptRoot
try {
    if ($Action -eq 'build') {
        & $nodeExecutable node_modules/typescript/bin/tsc -b
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $nodeExecutable node_modules/vite/bin/vite.js build
    } elseif ($Action -eq 'test') { & $nodeExecutable node_modules/vitest/vitest.mjs run }
    else { & $nodeExecutable node_modules/vite/bin/vite.js --host 127.0.0.1 }
    exit $LASTEXITCODE
} finally { Pop-Location }
