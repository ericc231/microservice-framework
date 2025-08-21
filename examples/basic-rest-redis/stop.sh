#!/bin/bash

# Basic REST Redis - 停止腳本
# 此腳本用於停止 basic-rest-redis 應用程式

echo "========================================="
echo "  Basic REST Redis - 停止中..."
echo "========================================="

# 尋找運行中的應用程式進程
APP_NAME="basic-rest-redis"
SPRING_APP_NAME="BasicRestRedisApplication"

echo "正在查找運行中的應用程式進程..."

# 方法 1: 使用應用程式名稱查找
PID=$(ps aux | grep "$SPRING_APP_NAME" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    # 方法 2: 使用 Maven 進程查找
    PID=$(ps aux | grep "spring-boot:run" | grep "$APP_NAME" | grep -v grep | awk '{print $2}')
fi

if [ -z "$PID" ]; then
    # 方法 3: 使用端口查找
    PID=$(lsof -t -i:8083 2>/dev/null)
fi

if [ -n "$PID" ]; then
    echo "發現應用程式進程 (PID: $PID)"
    
    # 嘗試正常關閉
    echo "正在嘗試正常關閉應用程式..."
    kill -TERM $PID
    
    # 等待應用程式關閉
    for i in {1..30}; do
        if ! kill -0 $PID 2>/dev/null; then
            echo "應用程式已成功停止"
            break
        fi
        echo "等待應用程式關閉... ($i/30)"
        sleep 1
    done
    
    # 如果仍在運行，強制終止
    if kill -0 $PID 2>/dev/null; then
        echo "正在強制終止應用程式..."
        kill -9 $PID
        
        # 再次檢查
        sleep 2
        if kill -0 $PID 2>/dev/null; then
            echo "錯誤: 無法停止應用程式 (PID: $PID)"
            exit 1
        else
            echo "應用程式已強制停止"
        fi
    fi
else
    echo "未找到運行中的應用程式進程"
    echo "可能應用程式已經停止，或使用不同的方式啟動"
fi

echo ""
echo "清理檢查..."

# 檢查端口是否仍被占用
if command -v lsof > /dev/null; then
    PORT_USAGE=$(lsof -i:8083 2>/dev/null)
    if [ -n "$PORT_USAGE" ]; then
        echo "警告: 端口 8083 仍被以下進程占用:"
        echo "$PORT_USAGE"
    else
        echo "端口 8083 已釋放"
    fi
fi

# 清理臨時檔案
if [ -f "application.pid" ]; then
    echo "清理 PID 檔案..."
    rm -f application.pid
fi

echo ""
echo "應用程式停止完成"
echo "========================================="