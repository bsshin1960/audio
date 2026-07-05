$adb = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 1. 디바이스 전체 정보 및 상태 점검" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
& $adb devices
& $adb shell getprop ro.product.model
& $adb shell getprop ro.build.version.release

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 2. 스마트폰 내부 포트 5277 리스닝 상태 점검" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
# netstat 또는 ss 명령을 이용해 스마트폰에서 5277 포트가 열려있는지 확인
$netstat = & $adb shell "netstat -an | grep 5277" 2>&1
$ss = & $adb shell "ss -tln | grep 5277" 2>&1
Write-Host "netstat 결과: $netstat"
Write-Host "ss 결과: $ss"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 3. Android Auto (gearhead) 패키지 및 서비스 점검" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
& $adb shell "dumpsys package com.google.android.projection.gearhead" | findstr "versionName"

# 실행 중인 서비스 상태 상세 확인
$services = & $adb shell "dumpsys activity services com.google.android.projection.gearhead" 2>&1
$services | Select-String -Pattern "DeveloperHeadUnitNetworkService|active|ServiceRecord" | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 4. ADB 포트 포워딩 전체 리스트" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
& $adb forward --list
