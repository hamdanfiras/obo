# OBO Token Exchange and Event Delegation PoC

This Proof of Concept demonstrates secure On-Behalf-Of (OBO) token exchange and event-type scoped delegation between microservices, compliant with OAuth2 RFC 8693 (Token Exchange).

## Architecture Overview

The PoC consists of five Spring Boot microservices:

1. **sts-service** - Security Token Service that issues and validates JWTs using Nimbus JOSE + JWT
2. **gateway-service** - Entry point that validates user JWTs and exchanges them for OBO tokens
3. **payments-service** - Validates OBO tokens, issues short-lived event OBO tokens, and publishes events to ActiveMQ
4. **payments-worker-service** - Consumes events, validates event OBO tokens, and exchanges them for downstream OBO tokens
5. **downstream-service-c** - Final service that validates OBO tokens

### Communication Flows

#### Synchronous Flow (HTTP → gRPC)
```
User → Gateway (user JWT) → STS (OBO exchange) → Payments Service (gRPC with OBO)
```

#### Asynchronous Flow (Event Delegation)
```
Payments Service → ActiveMQ (event with event OBO) → Worker → STS (OBO exchange) → Downstream Service C (gRPC with OBO)
```

## Key Features

- **RFC 8693 Token Exchange**: All token exchanges follow the OAuth2 Token Exchange specification
- **Strict OBO Chaining**: Each service validates and chains OBO tokens with actor (`act`) and chain tracking
- **Event-Type Scoped Tokens**: Short-lived (30s) event OBO tokens with `evt_type` claim for async messaging
- **Spring Security Integration**: Uses Spring Security OAuth2 Client for automatic token exchange
- **gRPC Security**: Custom interceptors for attaching OBO tokens to gRPC calls
- **ActiveMQ Integration**: Event-driven architecture with OBO tokens embedded in messages

## Prerequisites

- Docker and Docker Compose
- Maven 3.9+
- Java 21+

## Building and Running

### Build All Services

```bash
mvn clean install -DskipTests
```

### Run with Docker Compose

```bash
docker-compose up --build
```

This will start all services with the `docker` profile:
- STS Service: http://localhost:8081
- Gateway Service: http://localhost:8082
- Payments Service: http://localhost:8083 (HTTP), localhost:9093 (gRPC)
- Worker Service: http://localhost:8084
- Downstream Service: http://localhost:8085 (HTTP), localhost:9095 (gRPC)
- ActiveMQ: tcp://localhost:61616, http://localhost:8161 (admin console)

### Run Locally (Development Mode)

The application supports two Spring profiles:
- **`dev`** (default): For running services locally outside Docker
- **`docker`**: For running services inside Docker containers

To run services locally:

1. **Start ActiveMQ in Docker** (required for both profiles):
   ```bash
   docker-compose up activemq
   ```

2. **Start each service locally** (in separate terminals):
   ```bash
   # Terminal 1: STS Service
   cd sts-service
   mvn spring-boot:run
   
   # Terminal 2: Gateway Service
   cd gateway-service
   mvn spring-boot:run
   
   # Terminal 3: Payments Service
   cd payments-service
   mvn spring-boot:run
   
   # Terminal 4: Worker Service
   cd payments-worker-service
   mvn spring-boot:run
   
   # Terminal 5: Downstream Service
   cd downstream-service-c
   mvn spring-boot:run
   ```

   Services will use the `dev` profile by default, which configures:
   - ActiveMQ connection: `tcp://localhost:61616`
   - STS token URI: `http://localhost:8081/oauth2/token`
   - gRPC endpoints: `localhost:9093` (payments), `localhost:9095` (downstream)

3. **Access services** (same ports as Docker):
   - STS Service: http://localhost:8081
   - Gateway Service: http://localhost:8082
   - Payments Service: http://localhost:8083 (HTTP), localhost:9093 (gRPC)
   - Worker Service: http://localhost:8084
   - Downstream Service: http://localhost:8085 (HTTP), localhost:9095 (gRPC)

**Note**: Ports are unified between `dev` and `docker` profiles for consistency. Make sure to start them in the correct order (STS first, then gateway/payments, then worker/downstream).

## Testing the Flow

### 1. Generate a User JWT

First, create a user JWT token. You can use the provided utility class or create one manually:

```java
import com.example.obo.common.JwtUtils;

SignedJWT userJwt = JwtUtils.createUserJwt("user-123", "user@example.com");
String token = userJwt.serialize();
```

Or use a simple script:

```bash
# Using a JWT tool or the utility class
```

### 2. Call the Gateway

When running with Docker Compose:
```bash
curl -X POST http://localhost:8082/api/payments/initiate \
  -H "Authorization: Bearer <user_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "currency": "USD",
    "merchant_id": "merchant-123"
  }'
```

When running locally (dev profile) - same port as Docker:
```bash
curl -X POST http://localhost:8082/api/payments/initiate \
  -H "Authorization: Bearer <user_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "currency": "USD",
    "merchant_id": "merchant-123"
  }'
```

### 3. Observe the Flow

The request will trigger:

1. **Gateway** validates user JWT
2. **Gateway** exchanges user JWT for OBO token (audience: `payments-service`, scope: `payments.initiate`)
3. **Gateway** calls Payments Service via gRPC with OBO token
4. **Payments Service** validates OBO token
5. **Payments Service** issues event OBO token (30s lifetime, `evt_type: PAYMENT_INITIATED`)
6. **Payments Service** publishes event to ActiveMQ with event OBO token
7. **Worker** consumes event and validates event OBO token
8. **Worker** exchanges event OBO for downstream OBO (audience: `downstream-service-c`, scope: `payments.finalize`)
9. **Worker** calls Downstream Service C via gRPC with OBO token
10. **Downstream Service C** validates OBO token and processes request

Check the logs of each service to see the token validation and exchange process.

## Token Structure

### User JWT
```json
{
  "iss": "https://mock-idp.example.com",
  "sub": "user-123",
  "scope": "openid profile payments.initiate",
  "exp": <timestamp>
}
```

### OBO Token (Synchronous)
```json
{
  "iss": "https://sts.internal",
  "sub": "user-123",
  "aud": "payments-service",
  "scope": "payments.initiate",
  "act": "service-b",
  "chain": "user->service-b",
  "exp": <timestamp>
}
```

### Event OBO Token (Asynchronous)
```json
{
  "iss": "https://sts.internal",
  "sub": "user-123",
  "aud": "payments-worker-service",
  "scope": "payments.process",
  "evt_type": "PAYMENT_INITIATED",
  "typ": "internal_delegation+jwt",
  "act": "payments-service",
  "chain": "user->payments-service",
  "exp": <timestamp> (30 seconds)
}
```

## Security Features

- **Token Validation**: All services validate JWT signature, expiration, audience, and scope
- **Short-Lived Tokens**: OBO tokens expire in 2 minutes, event OBO tokens in 30 seconds
- **Scope Restriction**: Each token exchange can only request scopes that are subsets of the original token
- **Audience Validation**: Strict audience checking ensures tokens are only used by intended services
- **Chain Tracking**: The `chain` claim tracks the delegation path for audit purposes

## Configuration

The application uses Spring profiles to configure different environments:
- **`dev`** profile (default): For local development outside Docker
- **`docker`** profile: For running in Docker containers (set via `SPRING_PROFILES_ACTIVE=docker` in docker-compose.yml)

### Profile-Specific Configuration

#### Dev Profile (Local Development)
- ActiveMQ: `tcp://localhost:61616`
- STS Token URI: `http://localhost:8081/oauth2/token`
- Service Ports (same as Docker):
  - STS Service: `8081` (HTTP)
  - Gateway Service: `8082` (HTTP)
  - Payments Service: `8083` (HTTP), `9093` (gRPC)
  - Worker Service: `8084` (HTTP)
  - Downstream Service: `8085` (HTTP), `9095` (gRPC)
- gRPC Clients:
  - Payments Service: `localhost:9093`
  - Downstream Service: `localhost:9095`

#### Docker Profile (Containerized)
- ActiveMQ: `tcp://activemq:61616`
- STS Token URI: `http://sts:8080/oauth2/token`
- gRPC Clients:
  - Payments Service: `payments:9090`
  - Downstream Service: `downstream:9090`

### Service Configuration

#### STS Service
- Port: 8080 (exposed as 8081 in Docker)
- Endpoint: `/oauth2/token`
- Secret: `0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF` (256-bit key for HS256, configured in code)

#### Gateway Service
- Port: 8080 (exposed as 8082 in Docker)
- OAuth2 Client: Configured for token exchange with STS
- gRPC Client: Connects to Payments Service (profile-based configuration)

#### Payments Service
- HTTP Port: 8080 (exposed as 8083 in Docker)
- gRPC Port: 9090 (exposed as 9093 in Docker)
- ActiveMQ: Profile-based connection (see above)

#### Worker Service
- Port: 8080 (exposed as 8084 in Docker)
- ActiveMQ: Consumes from `payment.events` queue (profile-based connection)
- gRPC Client: Connects to Downstream Service C (profile-based configuration)

#### Downstream Service C
- HTTP Port: 8080 (exposed as 8085 in Docker)
- gRPC Port: 9090 (exposed as 9095 in Docker)

## Development

### Project Structure

```
obo/
├── common-lib/              # Shared utilities, DTOs, gRPC protos
├── sts-service/            # Security Token Service
├── gateway-service/        # Gateway service
├── payments-service/       # Payments service
├── payments-worker-service/# Worker service
├── downstream-service-c/   # Downstream service
├── docker-compose.yml      # Docker Compose configuration
└── pom.xml                 # Parent POM
```

### Adding New Services

1. Create a new module in the parent POM
2. Add dependencies: `spring-boot-starter-oauth2-resource-server`, `spring-security-oauth2-client`
3. Configure OAuth2 Client for token exchange (see `OAuth2ClientConfig` in gateway/worker)
4. Configure JWT decoder with shared secret
5. Add gRPC interceptors if using gRPC

## Troubleshooting

### Token Exchange Fails
- Check STS service is running and accessible
- Verify client credentials in `application.yml`
- Check token expiration times

### gRPC Calls Fail
- Verify gRPC server is running on correct port
- Check OBO token is being attached in interceptor
- Validate audience matches service name

### Events Not Consumed
- Check ActiveMQ is running: `docker ps | grep activemq`
- Verify queue name matches: `payment.events`
- Check worker service logs for errors

## License

This is a Proof of Concept for demonstration purposes.

