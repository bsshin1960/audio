@echo off
echo ==========================================================
echo  GravityMusic DHU (Android Auto - Palisades Wide Layout)
echo ==========================================================
echo.

set ADB_PATH="C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\platform-tools\adb.exe"
set DHU_DIR="C:\Temp\Antigrvity\sbstasker\.build-env\android-sdk\extras\google\auto"

echo [1/5] Checking smartphone connection...
%ADB_PATH% devices > temp_devices.txt
findstr /C:"device" temp_devices.txt > nul
if %errorlevel% neq 0 (
    echo [WARNING] No connected smartphone detected via ADB.
    echo Please make sure:
    echo  1. Smartphone is connected via USB cable.
    echo  2. USB Debugging is turned ON in Developer Options.
    echo  3. 'Allow USB debugging' permission is granted on the phone screen.
    del temp_devices.txt
    echo.
    pause
    exit /b
)
del temp_devices.txt
echo - Smartphone connected successfully!
echo.

echo [2/5] Closing any running DHU emulator instances...
taskkill /F /IM desktop-head-unit.exe >nul 2>&1

echo.
echo [3/5] Binding ADB Port Forwarding (tcp:5277)...
%ADB_PATH% forward --remove-all >nul 2>&1
%ADB_PATH% forward tcp:5277 tcp:5277
echo - Port forwarding completed (tcp:5277)
echo.

echo [4/5] Activating Head Unit Server on smartphone...
echo - Sending activation intent...
%ADB_PATH% shell am startservice --user 0 -n "com.google.android.projection.gearhead/.companion.DeveloperHeadUnitNetworkService" >nul 2>&1
echo.

echo ==========================================================
echo  [IMPORTANT TIP]
echo  If the DHU window shows "Waiting for phone..." or stays black:
echo   1. Please unlock your smartphone screen.
echo   2. Look for any Android Auto connection popup and tap "Agree/Start".
echo   3. Or manually open Android Auto Settings on the phone,
echo      tap top-right 3-dots and select "Start Head Unit Server".
echo ==========================================================
echo.

echo [5/5] Launching Desktop Head Unit with Palisades Spec...
cd /d %DHU_DIR%
desktop-head-unit.exe -c config\default_wide.ini -a 5277

echo.
echo DHU Session Closed.
pause
