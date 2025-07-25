#!/bin/bash

# --- Microservice Framework Start Script ---

# --- Configuration ---
APP_NAME="microservice-app"
APP_JAR="./$APP_NAME-0.0.1-SNAPSHOT.jar"
PID_FILE="$APP_NAME.pid"
LOG_FILE="../logs/$APP_NAME.log"

# --- Functions ---
start() {
    echo "Starting $APP_NAME..."
    if [ -f $PID_FILE ]; then
        echo "Error: $APP_NAME is already running (PID file exists)." 
        exit 1
    fi

    # Create logs directory if it doesn't exist
    mkdir -p ../logs

    # Default Java options
    JAVA_OPTS="-Xms256m -Xmx512m"

    # Check for startup mode
    if [ "$1" == "debug" ]; then
        echo "Starting in DEBUG mode."
        JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
    elif [ "$1" == "otel" ]; then
        echo "Starting with OpenTelemetry Agent."
        if [ ! -f "opentelemetry-javaagent.jar" ]; then
            echo "Error: opentelemetry-javaagent.jar not found in the bin directory."
            exit 1
        fi
        JAVA_OPTS="$JAVA_OPTS -javaagent:opentelemetry-javaagent.jar -Dotel.service.name=$APP_NAME"
    else
        echo "Starting in NORMAL mode."
    fi

    # Start the application in the background
    nohup java $JAVA_OPTS -Dloader.path=extensions/ -jar $APP_JAR > $LOG_FILE 2>&1 &

    # Store the process ID
    echo $! > $PID_FILE
    echo "$APP_NAME started with PID $(cat $PID_FILE). Log file: $LOG_FILE"
}

# --- Script Execution ---
start $1
