$adb = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"
$dhuDir = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\extras\google\auto"

# 1. 포트 포워딩
Write-Host "[1/4] ADB 포트 포워딩..." -ForegroundColor Cyan
& $adb forward tcp:5277 tcp:5277

# 2. gearhead PID 목록 수집
Write-Host "[2/4] Android Auto 프로세스 PID 수집..." -ForegroundColor Cyan
& $adb logcat -c
Start-Sleep -Milliseconds 300

$psOutput = & $adb shell "ps -A | grep gearhead" 2>&1
$pids = @()
foreach ($line in $psOutput) {
    if ($line -match '\s+(\d+)\s+') {
        $pids += $Matches[1]
    }
}
Write-Host "gearhead PIDs: $($pids -join ', ')"

# 3. logcat 백그라운드 캡처 시작
Write-Host "[3/4] 로그 캡처 시작 후 DHU 실행..." -ForegroundColor Cyan
$logFile = "C:\Temp\Antigrvity\audio\dhu_logcat.txt"
$logJob = Start-Job -ScriptBlock {
    param($adbPath, $outFile)
    & $adbPath logcat -v time *:V 2>&1 | Out-File -FilePath $outFile -Encoding UTF8
} -ArgumentList $adb, $logFile

Start-Sleep -Milliseconds 500

# 4. DHU 실행 (5초 타임아웃)
$dhuProcess = Start-Process -FilePath "$dhuDir\desktop-head-unit.exe" `
    -ArgumentList "-c", "config\default.ini" `
    -WorkingDirectory $dhuDir `
    -PassThru -NoNewWindow
    
Write-Host "DHU PID: $($dhuProcess.Id) - 연결 대기 중 (8초)..."
Start-Sleep -Seconds 8

# 5. 정리
$dhuProcess | Stop-Process -Force -ErrorAction SilentlyContinue
Stop-Job $logJob -ErrorAction SilentlyContinue
Remove-Job $logJob -Force -ErrorAction SilentlyContinue
& $adb forward --remove tcp:5277

# 6. 결과 분석
Write-Host ""
Write-Host "=== Android Auto / DHU 관련 로그 분석 ===" -ForegroundColor Yellow
if (Test-Path $logFile) {
    $logs = Get-Content $logFile -ErrorAction SilentlyContinue
    Write-Host "총 로그 라인: $($logs.Count)"
    Write-Host ""
    Write-Host "--- gearhead / projection 관련 ---" -ForegroundColor Green
    $logs | Where-Object { $_ -match "gearhead|projection|HeadUnit|DeveloperHead|aa_adb|AndroidAuto" } | Select-Object -Last 50
    Write-Host ""
    Write-Host "--- error/disconnect/version 관련 ---" -ForegroundColor Red
    $logs | Where-Object { $_ -match "disconnect|version.*mismatch|error.*head|head.*error|ssl.*error|reject" -and $_ -notmatch "CamX|camera" } | Select-Object -Last 20
} else {
    Write-Host "로그 파일을 찾을 수 없습니다."
}
