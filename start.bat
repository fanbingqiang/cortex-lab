@echo off
chcp 65001 >nul
echo ========================================
echo   Cortex 多智能体协作框架
echo ========================================
echo.

REM 设置API Key（请替换为你自己的Key）
set DEEPSEEK_API_KEY=your-api-key-here
set OPENAI_API_KEY=your-api-key-here

echo 正在启动...
echo.

REM 检查Maven是否存在
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven，请先安装Maven
    pause
    exit /b 1
)

REM 启动项目
mvn spring-boot:run

pause
