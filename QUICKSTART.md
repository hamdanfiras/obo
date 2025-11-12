# Quick Start Guide

## Prerequisites

- Docker and Docker Compose
- Maven 3.9+ (for local development)
- Java 21+

## Starting the Services

1. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Wait for all services to start** (check logs for "Started ...Application")

3. **Verify services are running:**
   ```bash
   curl http://localhost:8081/actuator/health  # STS
   curl http://localhost:8082/actuator/health  # Gateway
   curl http://localhost:8083/actuator/health  # Payments
   curl http://localhost:8084/actuator/health  # Worker
   curl http://localhost:8085/actuator/health  # Downstream
   ```

## Generating a User JWT

You need a user JWT token to test the flow. You can generate one using:

### Option 1: Using jwt.io (Easiest)

1. Go to https://jwt.io
2. Select algorithm: **HS256**
3. Enter secret: `0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF`
4. Use this payload:
   ```json
   {
     "iss": "https://mock-idp.example.com",
     "sub": "user-123",
     "email": "user@example.com",
     "scope": "openid profile payments.initiate",
     "exp": 9999999999
   }
   ```
5. Copy the generated token

### Option 2: Using Java Code

Create a simple Java class using the `JwtUtils` from common-lib:

```java
import com.example.obo.common.JwtUtils;
import com.nimbusds.jwt.SignedJWT;

SignedJWT jwt = JwtUtils.createUserJwt("user-123", "user@example.com");
String token = jwt.serialize();
System.out.println(token);
```

## Testing the Flow

### Using curl

```bash
curl -X POST http://localhost:8082/api/payments/initiate \
  -H "Authorization: Bearer <YOUR_USER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "currency": "USD",
    "merchant_id": "merchant-123"
  }'
```

### Using the test script

```bash
./scripts/test-flow.sh
```

## Expected Flow

1. **Gateway** receives user JWT and validates it
2. **Gateway** exchanges user JWT for OBO token (audience: `payments-service`)
3. **Gateway** calls Payments Service via gRPC with OBO token
4. **Payments Service** validates OBO token
5. **Payments Service** issues event OBO token (30s lifetime, `evt_type: PAYMENT_INITIATED`)
6. **Payments Service** publishes event to ActiveMQ with event OBO token
7. **Worker** consumes event and validates event OBO token
8. **Worker** exchanges event OBO for downstream OBO (audience: `downstream-service-c`)
9. **Worker** calls Downstream Service C via gRPC with OBO token
10. **Downstream Service C** validates OBO token and processes request

## Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f gateway
docker-compose logs -f payments
docker-compose logs -f worker
```

## Troubleshooting

### Services won't start
- Check Docker is running: `docker ps`
- Check ports are available: `lsof -i :8081-8085`
- Review logs: `docker-compose logs <service-name>`

### Token exchange fails
- Verify STS is accessible: `curl http://localhost:8081/oauth2/token`
- Check client credentials in `application.yml`
- Verify token hasn't expired

### gRPC calls fail
- Check gRPC ports are exposed: 9093, 9095
- Verify OBO token is being attached in logs
- Check audience matches service name

### Events not consumed
- Check ActiveMQ is running: `docker ps | grep activemq`
- Verify queue name: `payment.events`
- Check worker logs for errors

## Stopping Services

```bash
docker-compose down
```

To remove volumes and rebuild:

```bash
docker-compose down -v
docker-compose up --build
```

