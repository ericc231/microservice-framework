#!/bin/bash

# Basic Auth REST Service Start Script
SERVICE_NAME="basic-auth-rest"
SERVICE_PORT="8081"
JAR_FILE="${SERVICE_NAME}.jar"
PID_FILE="${SERVICE_NAME}.pid"
LOG_FILE="logs/${SERVICE_NAME}.log"

# Create logs directory
mkdir -p logs

# Check if service is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "Service $SERVICE_NAME is already running with PID $PID"
        exit 1
    else
        echo "Removing stale PID file"
        rm -f "$PID_FILE"
    fi
fi

# Set Java options
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${ENVIRONMENT:-development}"
JAVA_OPTS="$JAVA_OPTS -Dserver.port=${SERVICE_PORT}"
JAVA_OPTS="$JAVA_OPTS -Dlogging.file.name=${LOG_FILE}"

# Check for different startup modes
case "$1" in
    "debug")
        echo "Starting $SERVICE_NAME in DEBUG mode..."
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
        ;;
    "profile")
        echo "Starting $SERVICE_NAME in PROFILING mode..."
        JAVA_OPTS="$JAVA_OPTS -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=${SERVICE_NAME}-profile.jfr"
        ;;
    *)
        echo "Starting $SERVICE_NAME in NORMAL mode..."
        ;;
esac

# Start the service
echo "Starting Basic Auth REST Service..."
echo "Port: $SERVICE_PORT"
echo "Log file: $LOG_FILE"
echo "Java options: $JAVA_OPTS"

nohup java $JAVA_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
PID=$!

# Save PID
echo $PID > "$PID_FILE"

echo "Service started with PID: $PID"
echo "To view logs: tail -f $LOG_FILE"
echo "To stop service: ./stop.sh"

# Wait a moment and check if service started successfully
sleep 3
if ps -p $PID > /dev/null 2>&1; then
    echo "Service is running successfully!"
    
    # Test the service
    echo "Testing service availability..."
    for i in {1..10}; do
        if curl -s http://localhost:$SERVICE_PORT/actuator/health > /dev/null; then
            echo "Service is responding to health checks!"
            break
        fi
        if [ $i -eq 10 ]; then
            echo "Warning: Service may not be responding to health checks"
        fi
        sleep 2
    done
else
    echo "Failed to start service. Check logs: $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi