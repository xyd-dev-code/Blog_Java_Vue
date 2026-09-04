@echo off
echo ========================================
echo   Starting Blog Backend (Spring Boot)
echo ========================================
cd /d "%~dp0blog-server"

set JAVA_HOME=D:\AppData\Java\JDK\JDK17
set PATH=%JAVA_HOME%\bin;%PATH%

rem Local secrets may live in start-backend.local.bat, which is git-ignored.
rem When it is absent, this launcher asks for the MySQL password without echoing
rem it and generates a temporary JWT key, so the tracked script remains usable.

if exist "%~dp0start-backend.local.bat" (
    call "%~dp0start-backend.local.bat"
    echo [INFO] Loaded start-backend.local.bat
) else (
    echo [INFO] start-backend.local.bat not found; using interactive local setup.
)

rem The double-click launcher is for local development. A local file or an
rem already-defined environment variable can still override this value.
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=dev"
if not defined DB_USERNAME set "DB_USERNAME=root"

if not defined DB_PASSWORD (
    echo.
    echo MySQL account: %DB_USERNAME%@localhost
    for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "$s=Read-Host 'Enter MySQL password' -AsSecureString; $b=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try {[Runtime.InteropServices.Marshal]::PtrToStringBSTR($b)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b)}"`) do set "DB_PASSWORD=%%P"
)
if not defined DB_PASSWORD (
    echo.
    echo [ERROR] MySQL password was empty. Backend startup cancelled.
    echo         Run this file again and enter the password for %DB_USERNAME%@localhost.
    pause
    exit /b 1
)

if not defined BLOG_JWT_SECRET (
    for /f "usebackq delims=" %%S in (`powershell -NoProfile -Command "$r=[Security.Cryptography.RandomNumberGenerator]::Create(); $b=New-Object byte[] 48; try {$r.GetBytes($b); [Convert]::ToBase64String($b)} finally {$r.Dispose()}"`) do set "BLOG_JWT_SECRET=%%S"
    echo [INFO] Generated a temporary JWT key for this run.
    echo        To keep login sessions across restarts, copy start-backend.local.bat.example
    echo        to start-backend.local.bat and set a persistent BLOG_JWT_SECRET.
)
if not defined BLOG_JWT_SECRET (
    echo [ERROR] Failed to generate BLOG_JWT_SECRET. Backend startup cancelled.
    pause
    exit /b 1
)

rem Safety check: make sure we are inside the Maven project, otherwise the
rem spring-boot plugin prefix cannot be resolved and mvn fails with a cryptic error.
if not exist "pom.xml" (
    echo [ERROR] pom.xml not found in %CD%. The cd into blog-server may have failed.
    pause
    exit /b 1
)

echo Using JAVA_HOME=%JAVA_HOME%
echo Spring profile=%SPRING_PROFILES_ACTIVE%
echo Database user=%DB_USERNAME%
java -version

if /I "%~1"=="--check" (
    echo [OK] Backend startup configuration is ready.
    exit /b 0
)

echo.
echo Starting server at http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.

call mvn spring-boot:run

pause
