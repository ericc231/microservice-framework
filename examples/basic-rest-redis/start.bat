@echo off
setlocal enabledelayedexpansion

REM Basic REST Redis - Windows 啟動腳本
REM 此腳本用於在 Windows 上啟動 basic-rest-redis 示範應用程式

echo =========================================
echo   Basic REST Redis - 啟動中...
echo =========================================

REM 檢查 Java 版本
echo 檢查 Java 版本...
java -version >nul 2>&1
if !errorlevel! neq 0 (
    echo 錯誤: 未找到 Java，請安裝 Java 17 或更新版本
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)
echo 發現 Java 版本: !JAVA_VERSION!

REM 檢查 Maven
echo 檢查 Maven...
mvn --version >nul 2>&1
if !errorlevel! neq 0 (
    echo 錯誤: 未找到 Maven，請安裝 Maven 3.6 或更新版本
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('mvn --version 2^>^&1 ^| findstr "Apache Maven"') do (
    echo 發現 Maven: %%i
    goto :maven_found
)
:maven_found

REM 檢查 Redis 連線
echo 檢查 Redis 連線...
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost
if "%REDIS_PORT%"=="" set REDIS_PORT=6379

REM 嘗試使用 redis-cli 檢查連線 (如果可用)
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% ping >nul 2>&1
if !errorlevel! equ 0 (
    echo Redis 連線正常 ^(%REDIS_HOST%:%REDIS_PORT%^)
) else (
    echo 警告: 無法連接到 Redis ^(%REDIS_HOST%:%REDIS_PORT%^)
    echo 請確保 Redis 伺服器正在運行或執行以下命令啟動 Redis:
    echo   docker run -d --name redis-server -p 6379:6379 redis:latest
    echo 或安裝並啟動 Redis for Windows
    echo.
    echo 繼續啟動應用程式...
)

REM 設定環境變數
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=default
if "%JAVA_OPTS%"=="" set JAVA_OPTS=-Xms256m -Xmx512m

echo.
echo 環境設定:
echo   Profile: %SPRING_PROFILES_ACTIVE%
echo   Java Opts: %JAVA_OPTS%
echo   Redis: %REDIS_HOST%:%REDIS_PORT%
echo.

REM 建置專案
echo 建置專案...
mvn clean compile -q
if !errorlevel! neq 0 (
    echo 錯誤: 專案建置失敗
    pause
    exit /b 1
)

echo 建置成功!
echo.

REM 啟動應用程式
echo 正在啟動 Basic REST Redis 應用程式...
echo 應用程式將在 http://localhost:8083 啟動
echo.
echo 可用的 API 端點:
echo   健康檢查: GET  /test/redis/health
echo   建立資料: POST /test/redis/create
echo   讀取資料: GET  /test/redis/{id}
echo   更新資料: PUT  /test/redis/update
echo   刪除資料: DELETE /test/redis/{id}
echo   API 說明: GET  /test/redis/metadata/{operation}
echo.
echo 按 Ctrl+C 停止應用程式
echo =========================================

REM 啟動應用程式
mvn spring-boot:run

echo.
echo 應用程式已停止
pause