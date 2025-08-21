# LDAP Provider Example

This example demonstrates how to configure the framework to act as an LDAP authentication server. It uses an embedded UnboundID LDAP server with a pre-configured LDIF file for user data and group management.

## Features

- **Embedded LDAP Server**: UnboundID LDAP server for testing
- **Pre-configured Users**: LDIF file with test users and groups
- **Group Management**: Organizational units and group memberships
- **TLS Support**: LDAPS configuration with keystore
- **Test Integration**: Perfect for testing LDAP authentication flows

**Embedded LDAP Server Configuration:**

*   **Base DN:** `dc=springframework,dc=org`
*   **LDIF Data:** Loaded from `src/main/resources/users.ldif`
*   **Port:** 8389 (default for embedded LDAP)
*   **TLS Enabled:** Configured to use `keystore.jks` for secure communication (LDAPS on port 636).

**LDAP Schema and Data (`src/main/resources/users.ldif`):**

```ldif
dn: ou=people,dc=springframework,dc=org
objectclass: top
objectclass: organizationalUnit
ou: people

dn: uid=admin,ou=people,dc=springframework,dc=org
objectclass: top
objectclass: person
objectclass: organizationalPerson
objectclass: inetOrgPerson
cn: admin
sn: admin
uid: admin
userPassword: {noop}admin

dn: uid=user,ou=people,dc=springframework,dc=org
objectclass: top
objectclass: person
objectclass: organizationalPerson
objectclass: inetOrgPerson
cn: user
sn: user
uid: user
userPassword: {noop}password
```

**Running the LDAP Provider:**

1.  **Ensure the entire project is built:**
    ```bash
    cd microservice-parent
    mvn clean install
    ```

2.  **Copy the LDAP Provider JAR to the `microservice-app` extensions directory:**
    ```bash
    cp examples/ldap-provider/target/ldap-provider-0.0.1-SNAPSHOT.jar microservice-app/extensions/
    ```

3.  **Start the LDAP Provider using its dedicated script:**
    ```bash
    cd examples/ldap-provider/bin
    ./start.sh
    ```
    This script will set the `MICROSERVICE_APP_EXTRA_CLASSPATH` environment variable and then navigate to `microservice-app/bin` to start the main application, which will then load the `ldap-provider.jar` from the specified classpath.

4.  **Test the endpoint (requires LDAP client and server setup):**
    The default endpoint for LDAP authentication is configured at `/auth/ldap` (POST). You would typically send authentication requests to this endpoint.

    Example using `curl` (replace `user:password` with actual credentials):
    ```bash
    curl -v -X POST http://localhost:8082/auth/ldap -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
    ```
    (Note: `dXNlcjpwYXNzd29yZA==` is base64 encoded for `user:password`)

5.  **Stop the LDAP Provider:**
    ```bash
    cd examples/ldap-provider/bin
    ./stop.sh
    ```
