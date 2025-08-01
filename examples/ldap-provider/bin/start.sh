#!/bin/bash

# --- LDAP Provider Start Script ---

# Get the absolute path of the ldap-provider JAR
SERVICE_JAR="$(realpath ../target/ldap-provider-0.0.1-SNAPSHOT.jar)"

# Navigate to the microservice-app/bin directory
cd ../../microservice-app/bin

# Set the environment variable for extra classpath
export MICROSERVICE_APP_EXTRA_CLASSPATH="${SERVICE_JAR}"

# Start the microservice-app
./start.sh
