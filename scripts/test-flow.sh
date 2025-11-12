#!/bin/bash

# Test script for OBO token exchange flow
# This script demonstrates the complete flow

echo "=========================================="
echo "OBO Token Exchange PoC - Test Flow"
echo "=========================================="
echo ""

# Check if services are running
echo "1. Checking if services are running..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "   ERROR: STS service is not running on port 8081"
    echo "   Please start services with: docker-compose up"
    exit 1
fi

if ! curl -s http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo "   ERROR: Gateway service is not running on port 8082"
    exit 1
fi

echo "   ✓ Services are running"
echo ""

# Generate user JWT (simplified - in production, use proper IDP)
echo "2. Generating user JWT..."
echo "   Note: In a real scenario, this would come from your identity provider"
echo "   For testing, you can use jwt.io with:"
echo "   - Secret: 0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
echo "   - Algorithm: HS256"
echo "   - Payload:"
echo '   {'
echo '     "iss": "https://mock-idp.example.com",'
echo '     "sub": "user-123",'
echo '     "email": "user@example.com",'
echo '     "scope": "openid profile payments.initiate",'
echo '     "exp": <future_timestamp>'
echo '   }'
echo ""

# Prompt for JWT token
read -p "3. Enter your user JWT token: " USER_JWT

if [ -z "$USER_JWT" ]; then
    echo "   ERROR: JWT token is required"
    exit 1
fi

echo ""
echo "4. Calling Gateway Service..."
echo "   POST http://localhost:8082/api/payments/initiate"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8082/api/payments/initiate \
  -H "Authorization: Bearer $USER_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "currency": "USD",
    "merchant_id": "merchant-123"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "   HTTP Status: $HTTP_CODE"
echo "   Response:"
echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✓ Payment initiated successfully!"
    echo ""
    echo "5. Check the logs to see:"
    echo "   - Gateway exchanging user JWT for OBO token"
    echo "   - Payments service validating OBO and issuing event OBO"
    echo "   - Worker consuming event and exchanging for downstream OBO"
    echo "   - Downstream service validating final OBO"
else
    echo "   ✗ Request failed"
    echo "   Check service logs for details"
fi

