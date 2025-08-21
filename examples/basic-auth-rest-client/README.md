# basic-auth-rest-client

This example listens via the framework REST connector (no custom controllers) and, when invoked, calls the basic-auth-rest service using Basic Authentication.

How it works:
- Inbound: framework.connectors.rest.enabled=true activates the DynamicRestController.
- Routing: framework.routing maps POST /client/call to the @DL("basic-auth-client.call") domain logic.
- Outbound: Domain logic uses RestTemplate with Basic Auth to call the configured basic-auth-rest endpoint.

Run
- Start the basic-auth-rest service (default http://localhost:8081)
- Start this client (default http://localhost:8085)

Request

```bash
curl -X POST http://localhost:8085/client/call \
  -H "Content-Type: application/json" \
  -d '{"message":"ping"}'
```

Configuration (application.yml)

```yaml
framework:
  connectors:
    rest:
      enabled: true
  routing:
    - processName: "basic-auth-client.call"
      triggers:
        - type: rest
          path: "/client/call"
          method: POST

client:
  target:
    base-url: http://localhost:8081
    endpoint-path: /api/basic-auth
    username: user
    password: password
```
