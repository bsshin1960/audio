Add-Type @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public class WindowEnum {
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll", CharSet=CharSet.Auto)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    public struct RECT { public int Left, Top, Right, Bottom; }
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    public static List<IntPtr> GetWindowsForPID(uint pid) {
        var list = new List<IntPtr>();
        EnumWindows((hWnd, lParam) => {
            uint winPid;
            GetWindowThreadProcessId(hWnd, out winPid);
            if (winPid == pid) list.Add(hWnd);
            return true;
        }, IntPtr.Zero);
        return list;
    }
}
"@

$proc = Get-Process 'desktop-head-unit' -ErrorAction SilentlyContinue
if (-not $proc) {
    Write-Host "desktop-head-unit.exe is NOT running."
    exit
}

Write-Host "=== DHU Process Info ==="
Write-Host "PID: $($proc.Id)"
Write-Host "MainWindowHandle: $($proc.MainWindowHandle)"
Write-Host "MainWindowTitle: $($proc.MainWindowTitle)"
Write-Host "Responding: $($proc.Responding)"

Write-Host ""
Write-Host "=== All Windows for DHU Process ==="
$windows = [WindowEnum]::GetWindowsForPID([uint32]$proc.Id)
Write-Host "Total windows: $($windows.Count)"

foreach ($hwnd in $windows) {
    $sb = New-Object System.Text.StringBuilder 256
    [WindowEnum]::GetWindowText($hwnd, $sb, 256) | Out-Null
    $visible = [WindowEnum]::IsWindowVisible($hwnd)
    $rect = New-Object WindowEnum+RECT
    [WindowEnum]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
    Write-Host "  HWND=$hwnd Title=[$($sb.ToString())] Visible=$visible Pos=($($rect.Left),$($rect.Top))-($($rect.Right),$($rect.Bottom))"
    
    # If window has a title (likely the main window), try to show it
    if ($sb.ToString() -ne "" -and -not $visible) {
        Write-Host "    -> Attempting to show hidden window..."
        [WindowEnum]::ShowWindow($hwnd, 9) | Out-Null
        [WindowEnum]::SetForegroundWindow($hwnd) | Out-Null
    }
    if ($sb.ToString() -ne "" -and $visible) {
        Write-Host "    -> Window IS visible. Bringing to front..."
        [WindowEnum]::ShowWindow($hwnd, 9) | Out-Null
        [WindowEnum]::SetForegroundWindow($hwnd) | Out-Null
    }
}
