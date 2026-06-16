@echo off
title Cortex - Start (Docker)

echo ========================================
echo   Cortex - Quick Start (Docker)
echo ========================================
echo.

:: Check Docker
where docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker not found.
    echo Install: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker Desktop is not running.
    pause
    exit /b 1
)

echo [1/2] Building project (host)...
call mvn clean package -DskipTests -B

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo [2/2] Starting all services...
docker compose up -d --build

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to start.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   All services started!
echo.
echo   Frontend: http://localhost:8081/lab/
echo.
echo   To stop:  stop.bat
echo ========================================
echo.
pause
