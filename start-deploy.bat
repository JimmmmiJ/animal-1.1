@echo off
chcp 65001 >nul 2>nul
cd /d "%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell.exe -ExecutionPolicy Bypass -NoProfile -Command "Start-Process powershell.exe -ArgumentList '-ExecutionPolicy Bypass -NoProfile -File ""%~dp0start-deploy.ps1""' -Verb RunAs"
    exit /b
)

powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-deploy.ps1"
