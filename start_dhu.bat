@echo off
echo ==========================================================
echo  GravityMusic DHU (Android Auto - Palisades Wide Layout)
echo ==========================================================
echo.
echo [1/3] Closing any existing DHU process...
taskkill /F /IM desktop-head-unit.exe >nul 2>&1

echo.
echo [2/3] Setting up ADB Port Forwarding (tcp:5277)...
"C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe" forward tcp:5277 tcp:5277

echo.
echo [3/3] Launching Desktop Head Unit with explicit ADB port...
cd /d "C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\extras\google\auto"
desktop-head-unit.exe -c config\default_wide.ini -a 5277

echo.
echo DHU Session Closed.
pause
