$adb = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"
$dhuDir = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\extras\google\auto"

Write-Host "=== [1/3] logcat 초기화 ===" -ForegroundColor Cyan
& $adb logcat -c
Start-Sleep -Milliseconds 500

Write-Host "=== [2/3] DHU 실행 (백그라운드) ===" -ForegroundColor Cyan
$dhuJob = Start-Job -ScriptBlock {
    param($dir)
    Set-Location $dir
    & ".\desktop-head-unit.exe" -c "config\default.ini" 2>&1
} -ArgumentList $dhuDir

Start-Sleep -Seconds 4

Write-Host "=== [3/3] Android Auto 관련 로그 캡처 ===" -ForegroundColor Cyan
$logOutput = & $adb logcat -d -t 200 2>&1

Write-Host ""
Write-Host "--- AndroidAuto / HeadUnit 관련 라인 ---" -ForegroundColor Yellow
$logOutput | Where-Object {
    $_ -match "gearhead|AndroidAuto|HeadUnit|DeveloperHead|AA_|projection|disconnect|version|ssl|DHU|error|fail" -and
    $_ -notmatch "^---"
} | Select-Object -Last 80

Write-Host ""
Write-Host "--- DHU 프로세스 출력 ---" -ForegroundColor Yellow
$dhuJob | Receive-Job
Stop-Job $dhuJob
Remove-Job $dhuJob -Force

# DHU 프로세스 종료
Stop-Process -Name "desktop-head-unit" -Force -ErrorAction SilentlyContinue
Write-Host "완료." -ForegroundColor Green
