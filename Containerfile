# Use an official OpenJDK image as base
FROM openjdk:21-jdk-slim

# Install openssl for JWT secret generation
RUN apt-get update && apt-get install -y openssl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy startup scripts and make them executable
COPY scripts/ /app/scripts/
RUN chmod +x /app/scripts/*.sh

# Copy project files
COPY target/*.jar /app/app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Default command uses the startup script
CMD ["/app/scripts/start.sh"]