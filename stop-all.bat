@echo off
setlocal
chcp 65001 >nul

set "ROOT=%~dp0"
set "BLOG_ROOT=%ROOT%"

echo ========================================
echo   Blog - Stop All Services
echo ========================================
echo.

rem Close the two console windows created by start-all.bat, including child processes.
taskkill /F /T /FI "WINDOWTITLE eq Blog Backend*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Blog Frontend*" >nul 2>&1
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$root = $env:BLOG_ROOT; Get-CimInstance Win32_Process ^| Where-Object { $_.Name -eq 'cmd.exe' -and ($_.CommandLine -like ('*' + $root + 'start-backend.bat*') -or $_.CommandLine -like ('*' + $root + 'start-frontend.bat*')) } ^| ForEach-Object { taskkill.exe /F /T /PID $_.ProcessId ^| Out-Null }" >nul 2>&1

rem Also clean up listeners started manually or left behind unexpectedly.
call :stop_port 8080
if errorlevel 1 goto :stop_failed

call :stop_port 5173
if errorlevel 1 goto :stop_failed

echo.
echo [OK] Backend and frontend have been stopped.
ping -n 3 127.0.0.1 >nul
exit /b 0

:stop_port
set "PORT=%~1"
set "PORT_WAS_IN_USE=0"

for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    echo [INFO] Stopping PID %%P on port %PORT% ...
    taskkill /F /T /PID %%P >nul 2>&1
    set "PORT_WAS_IN_USE=1"
)

if "%PORT_WAS_IN_USE%"=="0" echo [INFO] Port %PORT% is already free.

if "%PORT_WAS_IN_USE%"=="1" ping -n 2 127.0.0.1 >nul

netstat -ano -p tcp | findstr /R /C:":%PORT% .*LISTENING" >nul
if not errorlevel 1 (
    echo [ERROR] Port %PORT% could not be released. Please run this script as Administrator.
    exit /b 1
)

exit /b 0

:stop_failed
echo.
echo [FAILED] One or more services could not be stopped.
pause
exit /b 1
