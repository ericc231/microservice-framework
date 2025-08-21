#\!/bin/bash

# Set the JAR name and extension path
JAR_NAME="oidc-provider-0.0.1-SNAPSHOT.jar"
EXTENSION_PATH="../../examples/oidc-provider/target/$JAR_NAME"

# Set the extra classpath for the microservice-app
export MICROSERVICE_APP_EXTRA_CLASSPATH="$EXTENSION_PATH"

# Navigate to the microservice-app/bin directory
cd ../../microservice-app/bin

# Start the microservice-app with the OIDC Provider in the classpath
./start.sh $1
