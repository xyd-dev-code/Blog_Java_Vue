@echo off
echo ========================================
echo   Starting Blog Backend (Spring Boot)
echo ========================================
cd /d "%~dp0blog-server"

set JAVA_HOME=D:\AppData\Java\JDK\JDK17
set PATH=%JAVA_HOME%\bin;%PATH%

rem Local secrets (DB_PASSWORD / BLOG_JWT_SECRET) live in start-backend.local.bat,
rem which is git-ignored and never committed. If it is missing, the build falls
rem back to defaults; Spring will then fail to start because BLOG_JWT_SECRET is
rem empty. This is expected and reminds you to create the local file.

if exist "%~dp0start-backend.local.bat" (
    call "%~dp0start-backend.local.bat"
) else (
    echo.
    echo [WARN] start-backend.local.bat not found, falling back to defaults.
    echo        Create start-backend.local.bat ^(git-ignored^) and set:
    echo            set DB_PASSWORD=your_mysql_password
    echo            set BLOG_JWT_SECRET=any_long_random_string_min_32_chars
    echo        Spring will refuse to start without BLOG_JWT_SECRET.
    echo.
)

rem Safety check: make sure we are inside the Maven project, otherwise the
rem spring-boot plugin prefix cannot be resolved and mvn fails with a cryptic error.
if not exist "pom.xml" (
    echo [ERROR] pom.xml not found in %CD%. The cd into blog-server may have failed.
    pause
    exit /b 1
)

echo Using JAVA_HOME=%JAVA_HOME%
java -version

echo.
echo Starting server at http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.

call mvn spring-boot:run

pause
