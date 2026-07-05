$adb = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"

Write-Host "=== Android Auto 관련 로그 수집 ==="
$log = & $adb logcat -d -s "AndroidAuto" "HeadUnit" "AA_Service" "HU" "aa_adb" "desktop-head-unit" 2>&1
$log | Select-Object -Last 60 | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "=== AA 패키지 버전 ==="
& $adb shell dumpsys package com.google.android.projection.gearhead | findstr "versionName"

Write-Host ""
Write-Host "=== 현재 실행 중인 AA 프로세스 ==="
& $adb shell ps | findstr "gearhead"
