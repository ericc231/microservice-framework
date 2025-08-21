#!/bin/bash

# Basic REST Redis - 啟動腳本
# 此腳本用於啟動 basic-rest-redis 示範應用程式

echo "========================================="
echo "  Basic REST Redis - 啟動中..."
echo "========================================="

# 檢查 Java 版本
echo "檢查 Java 版本..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "發現 Java 版本: $JAVA_VERSION"
    
    # 檢查是否為 Java 17 或更新版本
    JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d '.' -f1)
    if [ "$JAVA_MAJOR_VERSION" -lt 17 ]; then
        echo "警告: 建議使用 Java 17 或更新版本"
    fi
else
    echo "錯誤: 未找到 Java，請安裝 Java 17 或更新版本"
    exit 1
fi

# 檢查 Maven
echo "檢查 Maven..."
if command -v mvn &> /dev/null; then
    echo "發現 Maven: $(mvn --version | head -n1)"
else
    echo "錯誤: 未找到 Maven，請安裝 Maven 3.6 或更新版本"
    exit 1
fi

# 檢查 Redis 連線
echo "檢查 Redis 連線..."
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}

if command -v redis-cli &> /dev/null; then
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ping > /dev/null 2>&1; then
        echo "Redis 連線正常 ($REDIS_HOST:$REDIS_PORT)"
    else
        echo "警告: 無法連接到 Redis ($REDIS_HOST:$REDIS_PORT)"
        echo "請確保 Redis 伺服器正在運行或執行以下命令啟動 Redis:"
        echo "  docker run -d --name redis-server -p 6379:6379 redis:latest"
        echo "或"
        echo "  redis-server"
        echo ""
        echo "繼續啟動應用程式..."
    fi
else
    echo "未安裝 redis-cli，跳過 Redis 連線檢查"
fi

# 設定環境變數
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-default}
export JAVA_OPTS="${JAVA_OPTS} -Xms256m -Xmx512m"

echo ""
echo "環境設定:"
echo "  Profile: $SPRING_PROFILES_ACTIVE"
echo "  Java Opts: $JAVA_OPTS"
echo "  Redis: $REDIS_HOST:$REDIS_PORT"
echo ""

# 建置專案
echo "建置專案..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "錯誤: 專案建置失敗"
    exit 1
fi

echo "建置成功!"
echo ""

# 啟動應用程式
echo "正在啟動 Basic REST Redis 應用程式..."
echo "應用程式將在 http://localhost:8083 啟動"
echo ""
echo "可用的 API 端點:"
echo "  健康檢查: GET  /test/redis/health"
echo "  建立資料: POST /test/redis/create"
echo "  讀取資料: GET  /test/redis/{id}"
echo "  更新資料: PUT  /test/redis/update"
echo "  刪除資料: DELETE /test/redis/{id}"
echo "  API 說明: GET  /test/redis/metadata/{operation}"
echo ""
echo "按 Ctrl+C 停止應用程式"
echo "========================================="

# 啟動應用程式
mvn spring-boot:run

echo ""
echo "應用程式已停止"