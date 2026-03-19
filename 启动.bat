@echo off
chcp 65001
echo ========================================
echo 线路预警服务启动脚本
echo ========================================
echo.

cd /d %~dp0

echo [1/3] 检查jar包...
if not exist "target\route-warning-service-1.0.0.jar" (
    echo 错误：jar包不存在，请先执行 mvn package
    pause
    exit /b 1
)

echo [2/3] 启动服务...
echo 服务地址: http://localhost:8080
echo 健康检查: http://localhost:8080/api/health
echo.
echo 按 Ctrl+C 停止服务
echo.

java -jar target\route-warning-service-1.0.0.jar

pause
