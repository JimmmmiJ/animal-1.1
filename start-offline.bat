@echo off
chcp 65001 >nul 2>nul
cd /d "%~dp0"

powershell.exe -NoExit -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-offline.ps1"
