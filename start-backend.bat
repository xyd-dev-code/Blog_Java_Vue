@echo off
echo ========================================
echo   Starting Blog Backend (Spring Boot)
echo ========================================
cd /d "%~dp0blog-server"

set JAVA_HOME=D:\AppData\Java\JDK\JDK17
set PATH=%JAVA_HOME%\bin;%PATH%

rem 本地真实密钥(包含 JWT secret / 数据库密码)统一放在 start-backend.local.bat,
rem 该文件被 .gitignore 屏蔽,不会进 GitHub 仓库。
rem 如果不存在,直接调用 mvn,Spring 会因 BLOG_JWT_SECRET 为空启动失败 — 这是预期行为,
rem 提醒你复制 start-backend.local.bat.example 为 start-backend.local.bat 并填入本地值。
if exist "%~dp0start-backend.local.bat" (
    call "%~dp0start-backend.local.bat"
) else (
    echo.
    echo [WARN] start-backend.local.bat not found, falling back to defaults.
    echo        Please copy start-backend.local.bat.example to start-backend.local.bat
    echo        and fill in local DB_PASSWORD / BLOG_JWT_SECRET.
    echo.
)

echo Using JAVA_HOME=%JAVA_HOME%
java -version

echo.
echo Starting server at http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.

call mvn spring-boot:run

pause