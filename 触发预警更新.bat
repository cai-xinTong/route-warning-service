@echo off
chcp 65001
echo ========================================
echo 手动触发预警更新
echo ========================================
echo.

echo 正在触发预警更新任务...
curl -X POST http://localhost:8080/api/warning/update

echo.
echo.
echo 任务已触发！
echo 请查看服务日志或数据库 warning_info 表查看结果
pause
