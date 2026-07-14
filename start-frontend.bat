@echo off
chcp 65001 >nul
echo ========================================
echo   Starting Blog Frontend (Vue 3 + Vite)
echo ========================================
cd /d "%~dp0blog-web"

if not exist "node_modules" (
  echo Installing dependencies...
  call npm install
)

echo.
echo Starting dev server at http://localhost:5173
echo.

call npm run dev

pause