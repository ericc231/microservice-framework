@echo off
setlocal enabledelayedexpansion

REM Basic REST Redis - Windows 停止腳本
REM 此腳本用於在 Windows 上停止 basic-rest-redis 應用程式

echo =========================================
echo   Basic REST Redis - 停止中...
echo =========================================

REM 尋找運行中的應用程式進程
set APP_NAME=basic-rest-redis
set SPRING_APP_NAME=BasicRestRedisApplication

echo 正在查找運行中的應用程式進程...

REM 方法 1: 使用應用程式名稱查找
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo table /nh ^| findstr "%SPRING_APP_NAME%"') do (
    set PID=%%i
    goto :found_pid
)

REM 方法 2: 使用命令行參數查找
for /f "tokens=2" %%i in ('wmic process where "commandline like '%%spring-boot:run%%' and commandline like '%%%APP_NAME%%%'" get processid /format:csv ^| findstr /r "[0-9]"') do (
    set PID=%%i
    goto :found_pid
)

REM 方法 3: 查找占用端口 8083 的進程
for /f "tokens=5" %%i in ('netstat -ano ^| findstr ":8083 "') do (
    set PID=%%i
    goto :found_pid
)

echo 未找到運行中的應用程式進程
echo 可能應用程式已經停止，或使用不同的方式啟動
goto :cleanup

:found_pid
if "%PID%"=="" (
    echo 未找到運行中的應用程式進程
    goto :cleanup
)

echo 發現應用程式進程 ^(PID: %PID%^)

REM 嘗試正常關閉
echo 正在嘗試正常關閉應用程式...
taskkill /pid %PID% /t >nul 2>&1

REM 等待應用程式關閉
set /a WAIT_COUNT=0
:wait_loop
tasklist /fi "pid eq %PID%" 2>nul | findstr "%PID%" >nul
if !errorlevel! neq 0 (
    echo 應用程式已成功停止
    goto :cleanup
)

set /a WAIT_COUNT+=1
if !WAIT_COUNT! gtr 30 (
    goto :force_kill
)

echo 等待應用程式關閉... ^(!WAIT_COUNT!/30^)
timeout /t 1 /nobreak >nul
goto :wait_loop

:force_kill
echo 正在強制終止應用程式...
taskkill /pid %PID% /f /t >nul 2>&1

REM 再次檢查
timeout /t 2 /nobreak >nul
tasklist /fi "pid eq %PID%" 2>nul | findstr "%PID%" >nul
if !errorlevel! equ 0 (
    echo 錯誤: 無法停止應用程式 ^(PID: %PID%^)
    pause
    exit /b 1
) else (
    echo 應用程式已強制停止
)

:cleanup
echo.
echo 清理檢查...

REM 檢查端口是否仍被占用
netstat -ano | findstr ":8083 " >nul 2>&1
if !errorlevel! equ 0 (
    echo 警告: 端口 8083 仍被以下進程占用:
    netstat -ano | findstr ":8083 "
) else (
    echo 端口 8083 已釋放
)

REM 清理臨時檔案
if exist "application.pid" (
    echo 清理 PID 檔案...
    del /f /q "application.pid" >nul 2>&1
)

echo.
echo 應用程式停止完成
echo =========================================
pause