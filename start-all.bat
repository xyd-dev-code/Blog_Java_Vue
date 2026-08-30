@echo off
setlocal
chcp 65001 >nul

set "ROOT=%~dp0"
set "BLOG_ROOT=%ROOT%"

echo ========================================
echo   Blog - Start All Services
echo ========================================
echo.

rem Close console windows left by a previous run of this script.
taskkill /F /T /FI "WINDOWTITLE eq Blog Backend*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Blog Frontend*" >nul 2>&1
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$root = $env:BLOG_ROOT; Get-CimInstance Win32_Process ^| Where-Object { $_.Name -eq 'cmd.exe' -and ($_.CommandLine -like ('*' + $root + 'start-backend.bat*') -or $_.CommandLine -like ('*' + $root + 'start-frontend.bat*')) } ^| ForEach-Object { taskkill.exe /F /T /PID $_.ProcessId ^| Out-Null }" >nul 2>&1

rem Release the fixed development ports before starting fresh processes.
call :stop_port 8080
if errorlevel 1 goto :start_failed

call :stop_port 5173
if errorlevel 1 goto :start_failed

if not exist "%ROOT%start-backend.bat" (
    echo [ERROR] start-backend.bat was not found.
    goto :start_failed
)

if not exist "%ROOT%start-frontend.bat" (
    echo [ERROR] start-frontend.bat was not found.
    goto :start_failed
)

echo [INFO] Starting backend on http://localhost:8080 ...
start "Blog Backend" cmd.exe /d /c call "%ROOT%start-backend.bat"

echo [INFO] Waiting for the backend to become ready ...
call :wait_for_port 8080 60
if errorlevel 1 (
    echo [ERROR] Backend did not open port 8080 within 60 seconds.
    echo         Check the Blog Backend window for the startup error.
    goto :start_failed
)

echo [INFO] Starting frontend on http://localhost:5173 ...
start "Blog Frontend" cmd.exe /d /c call "%ROOT%start-frontend.bat"

echo [INFO] Waiting for the frontend to become ready ...
call :wait_for_port 5173 30
if errorlevel 1 (
    echo [ERROR] Frontend did not open port 5173 within 30 seconds.
    echo         Check the Blog Frontend window for the startup error.
    goto :start_failed
)

echo.
echo [OK] Backend and frontend are ready.
echo      Backend and frontend logs are shown in separate windows.
echo.
ping -n 4 127.0.0.1 >nul
exit /b 0

:stop_port
set "PORT=%~1"
set "PORT_WAS_IN_USE=0"

for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    echo [INFO] Port %PORT% is occupied by PID %%P. Stopping it...
    taskkill /F /T /PID %%P >nul 2>&1
    set "PORT_WAS_IN_USE=1"
)

if "%PORT_WAS_IN_USE%"=="1" ping -n 2 127.0.0.1 >nul

netstat -ano -p tcp | findstr /R /C:":%PORT% .*LISTENING" >nul
if not errorlevel 1 (
    echo [ERROR] Port %PORT% could not be released. Please run this script as Administrator.
    exit /b 1
)

exit /b 0

:wait_for_port
set "WAIT_PORT=%~1"
set /a "WAIT_LIMIT=%~2"
set /a "WAITED=0"

:wait_for_port_loop
netstat -ano -p tcp | findstr /R /C:":%WAIT_PORT% .*LISTENING" >nul
if not errorlevel 1 exit /b 0

if %WAITED% GEQ %WAIT_LIMIT% exit /b 1
set /a "WAITED+=1"
ping -n 2 127.0.0.1 >nul
goto :wait_for_port_loop

:start_failed
echo.
echo [FAILED] Services were not started.
pause
exit /b 1
