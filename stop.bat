@echo off
title Cortex - Stop

echo Stopping all services...
docker compose down
echo.
echo Done. All services stopped.
echo To also remove data: docker compose down -v
echo.
pause
