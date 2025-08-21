#\!/bin/bash

# Set the JAR name and extension path
JAR_NAME="oidc-auth-rest-0.0.1-SNAPSHOT.jar"
EXTENSION_PATH="../../examples/oidc-auth-rest/target/$JAR_NAME"

# Set the extra classpath for the microservice-app
export MICROSERVICE_APP_EXTRA_CLASSPATH="$EXTENSION_PATH"

# Navigate to the microservice-app/bin directory
cd ../../microservice-app/bin

# Start the microservice-app with the OIDC Auth REST in the classpath
./start.sh $1
