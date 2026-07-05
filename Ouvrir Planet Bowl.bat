@echo off
title Planet Bowl - Serveur local
cd /d "%~dp0"

if not exist "node_modules" (
  echo Installation des dependances, patientez...
  call npm install
)

echo.
echo ==========================================
echo   Planet Bowl demarre...
echo   Le navigateur va s'ouvrir automatiquement.
echo   Laissez cette fenetre ouverte.
echo   Pour arreter : fermez cette fenetre.
echo ==========================================
echo.

start "" cmd /c "timeout /t 3 /nobreak >nul & start http://127.0.0.1:5173/"
call npm run dev
