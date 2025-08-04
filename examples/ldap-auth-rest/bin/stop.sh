#!/bin/bash

# LDAP Auth REST Service Stop Script
SERVICE_NAME="ldap-auth-rest"
PID_FILE="${SERVICE_NAME}.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. Service may not be running."
    exit 1
fi

PID=$(cat "$PID_FILE")

if ! ps -p $PID > /dev/null 2>&1; then
    echo "Service is not running (PID $PID not found)"
    rm -f "$PID_FILE"
    exit 1
fi

echo "Stopping LDAP Auth REST Service (PID: $PID)..."

# Try graceful shutdown first
kill $PID

# Wait for graceful shutdown
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "Service stopped gracefully"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill if graceful shutdown failed
echo "Graceful shutdown failed, forcing termination..."
kill -9 $PID

# Wait for force kill
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "Service stopped forcefully"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

echo "Failed to stop service. Manual intervention may be required."
exit 1