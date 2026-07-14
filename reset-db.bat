@echo off
chcp 65001 >nul
echo ========================================
echo   Reset & Init Blog Database
echo ========================================
set MYSQL_HOST=localhost
set MYSQL_USER=root
set MYSQL_PASS=BOOTSTRAP_REQUIRED

echo Dropping existing database (if any)...
mysql -h%MYSQL_HOST% -u%MYSQL_USER% -p%MYSQL_PASS% -e "DROP DATABASE IF EXISTS blog_java_vue;"

echo Importing schema and seed data...
mysql -h%MYSQL_HOST% -u%MYSQL_USER% -p%MYSQL_PASS% < "%~dp0blog-server\src\main\resources\db\init.sql"

if errorlevel 1 (
  echo.
  echo [ERROR] Init failed. Please check your MySQL credentials.
  pause
  exit /b 1
)

echo.
echo [OK] Database initialized successfully.
pause