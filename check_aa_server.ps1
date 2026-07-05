$adb = "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"

Write-Host "=== 현재 사용자 목록 ==="
& $adb shell pm list users

Write-Host ""
Write-Host "=== Android Auto 포트 소켓 상태 (user 0) ==="
& $adb shell --user 0 ss -tlnp 2>$null | Select-String "5277","gearhead"

Write-Host ""
Write-Host "=== Android Auto 포트 소켓 전체 ==="
& $adb shell cat /proc/net/tcp6 2>$null | Select-Object -First 5

Write-Host ""
Write-Host "=== AA 헤드유닛 서버 상태 확인 ==="
& $adb shell dumpsys activity services com.google.android.projection.gearhead 2>$null | Select-String -Pattern "HeadUnit","running","started","port","5277" -CaseSensitive:$false | Select-Object -First 20
