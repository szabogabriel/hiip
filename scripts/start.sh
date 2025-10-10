#!/bin/bash

# HIIP Application Startup Script
# This script provides different startup modes for the HIIP application

set -e

# Default values
GENERATE_JWT_SECRET_ON_START=${GENERATE_JWT_SECRET_ON_START:-false}
JAR_FILE=${JAR_FILE:-/app/app.jar}

echo "🚀 Starting HIIP Application..."

# Function to generate JWT secret
generate_jwt_secret() {
    echo "🔐 Generating new JWT secret..."
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    export HIIP_JWT_SECRET="$JWT_SECRET"
    echo "✅ JWT secret generated and set"
}

# Function to validate JWT secret
validate_jwt_secret() {
    if [ -z "$HIIP_JWT_SECRET" ]; then
        echo "❌ ERROR: HIIP_JWT_SECRET environment variable is not set!"
        echo ""
        echo "You can fix this by:"
        echo "1. Setting the environment variable: export HIIP_JWT_SECRET=\"your-secret-here\""
        echo "2. Generating a new secret: ./scripts/generate-jwt-secret.sh"
        echo "3. Using auto-generation: export GENERATE_JWT_SECRET_ON_START=true"
        echo ""
        exit 1
    fi
    
    # Check if secret is long enough (at least 64 characters for base64 encoded 512-bit key)
    if [ ${#HIIP_JWT_SECRET} -lt 64 ]; then
        echo "⚠️  WARNING: JWT secret might be too short for optimal security"
        echo "   Current length: ${#HIIP_JWT_SECRET} characters"
        echo "   Recommended: 64+ characters (512+ bits)"
    fi
}

# Main startup logic
main() {
    echo "Configuration:"
    echo "  JAR_FILE: $JAR_FILE"
    echo "  GENERATE_JWT_SECRET_ON_START: $GENERATE_JWT_SECRET_ON_START"
    echo "  HIIP_H2_CONSOLE_ENABLED: ${HIIP_H2_CONSOLE_ENABLED:-false}"
    echo ""

    # Handle JWT secret
    if [ "$GENERATE_JWT_SECRET_ON_START" = "true" ]; then
        echo "🔄 Auto-generating JWT secret on startup..."
        echo "⚠️  WARNING: This will invalidate all existing tokens!"
        generate_jwt_secret
    else
        echo "🔍 Validating existing JWT secret..."
        validate_jwt_secret
        echo "✅ JWT secret validation passed"
    fi

    echo ""
    echo "🎯 Starting Java application..."
    echo "📍 JAR: $JAR_FILE"
    echo ""
    
    # Start the application
    exec java -jar "$JAR_FILE"
}

# Handle script arguments
case "${1:-}" in
    "generate-secret")
        generate_jwt_secret
        echo "Secret generated. Use: export HIIP_JWT_SECRET=\"$HIIP_JWT_SECRET\""
        exit 0
        ;;
    "validate-secret")
        validate_jwt_secret
        echo "✅ JWT secret is valid"
        exit 0
        ;;
    "help"|"-h"|"--help")
        echo "HIIP Application Startup Script"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  (none)           Start the application"
        echo "  generate-secret  Generate a new JWT secret"
        echo "  validate-secret  Validate the current JWT secret"
        echo "  help            Show this help message"
        echo ""
        echo "Environment Variables:"
        echo "  HIIP_JWT_SECRET                Required JWT signing secret"
        echo "  GENERATE_JWT_SECRET_ON_START   Auto-generate secret on startup (default: false)"
        echo "  JAR_FILE                       Path to JAR file (default: /app/app.jar)"
        echo "  HIIP_H2_CONSOLE_ENABLED        Enable H2 console (default: false)"
        echo ""
        echo "Examples:"
        echo "  # Normal startup with existing secret"
        echo "  export HIIP_JWT_SECRET=\"your-secret-here\""
        echo "  $0"
        echo ""
        echo "  # Startup with auto-generated secret (not recommended for production)"
        echo "  export GENERATE_JWT_SECRET_ON_START=true"
        echo "  $0"
        exit 0
        ;;
    *)
        if [ -n "$1" ]; then
            echo "❌ Unknown command: $1"
            echo "Use '$0 help' for available commands"
            exit 1
        fi
        main
        ;;
esac
