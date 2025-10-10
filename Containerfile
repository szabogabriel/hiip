# Use an official OpenJDK image as base
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy project files
COPY target/*.jar /app/app.jar

# Default command (update JAR name as needed)
CMD ["java", "-jar", "app.jar"]