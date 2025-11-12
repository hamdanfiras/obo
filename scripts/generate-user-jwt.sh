#!/bin/bash

# Simple script to generate a user JWT for testing
# This requires Java and the common-lib to be built

echo "Generating user JWT token..."
echo ""
echo "To generate a user JWT programmatically, use:"
echo ""
echo "import com.example.obo.common.JwtUtils;"
echo "SignedJWT jwt = JwtUtils.createUserJwt(\"user-123\", \"user@example.com\");"
echo "String token = jwt.serialize();"
echo ""
echo "Or use a JWT tool like jwt.io with the following payload:"
echo ""
cat << 'EOF'
{
  "iss": "https://mock-idp.example.com",
  "sub": "user-123",
  "email": "user@example.com",
  "scope": "openid profile payments.initiate",
  "exp": <future_timestamp>
}
EOF
echo ""
echo "Secret: 0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
echo "Algorithm: HS256"

