#!/bin/bash

# --- Microservice Framework Stop Script ---

# --- Configuration ---
APP_NAME="microservice-framework"
PID_FILE="$APP_NAME.pid"

# --- Functions ---
stop() {
    echo "Stopping $APP_NAME..."
    if [ -f $PID_FILE ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null; then
            kill $PID
            # Wait for the process to terminate
            for i in {1..10};
            do
                if ! ps -p $PID > /dev/null; then
                    echo "$APP_NAME (PID $PID) stopped successfully."
                    rm $PID_FILE
                    return 0
                fi
                sleep 1
            done
            echo "Warning: $APP_NAME (PID $PID) did not stop gracefully. Attempting to force kill."
            kill -9 $PID
            rm $PID_FILE
        else
            echo "PID file found, but $APP_NAME (PID $PID) is not running. Cleaning up PID file."
            rm $PID_FILE
        fi
    else
        echo "$APP_NAME is not running (PID file not found)."
    fi
    return 0
}

# --- Script Execution ---
stop
