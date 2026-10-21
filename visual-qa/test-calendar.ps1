param([string]$Serial = 'emulator-5554', [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Calendar QA requires an emulator.' }
$taskAvd = (& $taskAdb -s $Serial emu avd name) -join ' '
if ($LASTEXITCODE -ne 0 -or $taskAvd -notmatch 'Pixel_7') { throw "Start the Pixel 7 AVD. Found: $taskAvd" }
Push-Location $taskRoot
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
    }
    & $taskAdb -s $Serial install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'App install failed.' }
    & $taskAdb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Test install failed.' }
    $taskEvidence = Join-Path $taskRoot 'visual-qa\evidence\calendar'
    New-Item -ItemType Directory -Force -Path $taskEvidence | Out-Null
    $taskOutput = & $taskAdb -s $Serial shell am instrument -w -r -e class com.jeremy.lumi.ui.screens.calendar.CalendarRedesignTest,com.jeremy.lumi.ui.screens.calendar.DailyLogSheetTest com.jeremy.lumi.test/androidx.test.runner.AndroidJUnitRunner
    $taskOutput | Set-Content -LiteralPath (Join-Path $taskEvidence 'pixel-7-tests.txt')
    $taskOutput | Write-Output
    & $taskAdb -s $Serial pull /storage/emulated/0/Android/data/com.jeremy.lumi/files/calendar-qa $taskEvidence
    if (($taskOutput -join "`n") -notmatch 'OK \(\d+ tests?\)') { throw 'UI checks failed.' }
} finally { Pop-Location }
