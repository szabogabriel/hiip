#!/bin/bash

# JWT Secret Generator for HIIP Application
# Generates a cryptographically secure 512-bit (64-byte) secret for HS512 algorithm

echo "Generating JWT secret for HIIP application..."

# Generate a 64-byte (512-bit) random secret and encode it in base64
SECRET=$(openssl rand -base64 64 | tr -d '\n')

echo "Generated JWT Secret:"
echo "HIIP_JWT_SECRET=$SECRET"
echo ""
echo "You can use this secret by:"
echo "1. Setting it as an environment variable:"
echo "   export HIIP_JWT_SECRET=\"$SECRET\""
echo ""
echo "2. Adding it to your .env file:"
echo "   HIIP_JWT_SECRET=$SECRET"
echo ""
echo "3. Using it with Docker:"
echo "   docker run -e HIIP_JWT_SECRET=\"$SECRET\" hiit"
echo ""
echo "⚠️  IMPORTANT: Store this secret securely and never commit it to version control!"
