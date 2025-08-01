#!/bin/bash

# --- Helloworld Service Start Script ---

# Get the absolute path of the helloworld-service JAR
SERVICE_JAR="$(realpath ../target/helloworld-service-0.0.1-SNAPSHOT.jar)"

# Navigate to the microservice-app/bin directory
cd ../../microservice-app/bin

# Set the environment variable for extra classpath
export MICROSERVICE_APP_EXTRA_CLASSPATH="${SERVICE_JAR}"

# Start the microservice-app
./start.sh
