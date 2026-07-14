@echo off
chcp 65001 >nul
echo ========================================
echo   Starting Blog Backend (Spring Boot)
echo ========================================
cd /d "%~dp0blog-server"

set JAVA_HOME=D:\AppData\Java\JDK\JDK17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using JAVA_HOME=%JAVA_HOME%
java -version

echo.
echo Starting server at http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.

call mvn spring-boot:run

pause