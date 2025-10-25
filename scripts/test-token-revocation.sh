#!/bin/bash

# Test script for token revocation (logout)
# This script will:
# 1. Login and get a token
# 2. Use the token to access data (should work)
# 3. Logout with the token
# 4. Try to use the same token again (should fail with 401)

BASE_URL="http://localhost:8080"
USERNAME="hiipa"
PASSWORD="hiipa"

echo "========================================="
echo "Token Revocation Test Script"
echo "========================================="
echo ""

# Step 1: Login
echo "Step 1: Logging in as ${USERNAME}..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

echo "Login response: ${LOGIN_RESPONSE}"
echo ""

# Extract token
TOKEN=$(echo ${LOGIN_RESPONSE} | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

if [ -z "$TOKEN" ]; then
    echo "❌ ERROR: Failed to extract token from login response"
    exit 1
fi

echo "✅ Token extracted: ${TOKEN:0:50}..."
echo ""

# Step 2: Access data with token (should work)
echo "Step 2: Accessing /api/v1/data with token (should work)..."
DATA_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${BASE_URL}/api/v1/data" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_CODE=$(echo "${DATA_RESPONSE}" | grep -o 'HTTP_CODE:[0-9]*' | grep -o '[0-9]*')
DATA_BODY=$(echo "${DATA_RESPONSE}" | sed 's/HTTP_CODE:[0-9]*//')

echo "HTTP Status: ${HTTP_CODE}"
echo "Response: ${DATA_BODY:0:200}..."
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ SUCCESS: Token works before logout"
else
    echo "❌ ERROR: Token should work before logout but got HTTP ${HTTP_CODE}"
    exit 1
fi
echo ""

# Step 3: Logout
echo "Step 3: Logging out (revoking token)..."
LOGOUT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "${BASE_URL}/api/v1/auth/logout" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_CODE=$(echo "${LOGOUT_RESPONSE}" | grep -o 'HTTP_CODE:[0-9]*' | grep -o '[0-9]*')
LOGOUT_BODY=$(echo "${LOGOUT_RESPONSE}" | sed 's/HTTP_CODE:[0-9]*//')

echo "HTTP Status: ${HTTP_CODE}"
echo "Response: ${LOGOUT_BODY}"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Logout successful"
else
    echo "⚠️  WARNING: Logout returned HTTP ${HTTP_CODE}"
fi
echo ""

# Wait a moment for the database to update
sleep 1

# Step 4: Try to access data again with the same token (should fail)
echo "Step 4: Trying to access /api/v1/data with revoked token (should fail)..."
DATA_RESPONSE_AFTER=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${BASE_URL}/api/v1/data" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_CODE_AFTER=$(echo "${DATA_RESPONSE_AFTER}" | grep -o 'HTTP_CODE:[0-9]*' | grep -o '[0-9]*')
DATA_BODY_AFTER=$(echo "${DATA_RESPONSE_AFTER}" | sed 's/HTTP_CODE:[0-9]*//')

echo "HTTP Status: ${HTTP_CODE_AFTER}"
echo "Response: ${DATA_BODY_AFTER:0:200}"
echo ""

echo "========================================="
echo "Test Results:"
echo "========================================="
echo "1. Login:                    ✅ Success"
echo "2. Access before logout:     ✅ Success (HTTP ${HTTP_CODE})"
echo "3. Logout:                   $([ "$HTTP_CODE" = "200" ] && echo "✅ Success" || echo "⚠️  HTTP ${HTTP_CODE}")"

if [ "$HTTP_CODE_AFTER" = "401" ] || [ "$HTTP_CODE_AFTER" = "403" ]; then
    echo "4. Access after logout:      ✅ SUCCESS - Token revoked (HTTP ${HTTP_CODE_AFTER})"
    echo ""
    echo "🎉 TOKEN REVOCATION IS WORKING CORRECTLY!"
elif [ "$HTTP_CODE_AFTER" = "200" ]; then
    echo "4. Access after logout:      ❌ FAILED - Token still works (HTTP ${HTTP_CODE_AFTER})"
    echo ""
    echo "❌ TOKEN REVOCATION IS NOT WORKING!"
    echo ""
    echo "Possible issues:"
    echo "- Application not restarted with new code"
    echo "- Database table not created (check revoked_tokens table)"
    echo "- Check application logs for errors"
else
    echo "4. Access after logout:      ⚠️  Unexpected HTTP code: ${HTTP_CODE_AFTER}"
fi
echo "========================================="
