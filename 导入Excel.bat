@echo off
chcp 65001
echo ========================================
echo 导入Excel数据
echo ========================================
echo.

set EXCEL_PATH=D:\ideaProjectNew\xianLuYUjing\路段经纬度.xlsx

echo Excel文件路径: %EXCEL_PATH%
echo.

if not exist "%EXCEL_PATH%" (
    echo 错误：Excel文件不存在
    echo 请修改脚本中的 EXCEL_PATH 变量
    pause
    exit /b 1
)

echo 正在导入数据，请稍候...
curl -X POST "http://localhost:8080/api/import/excel?filePath=%EXCEL_PATH%"

echo.
echo.
echo 导入完成！
pause
