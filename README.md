# hiip
A simple universal data storage.

## Overview

Hiip is a Spring Boot application that provides a REST API for storing data with tags. Each user can store, search, and manage their own data with tag-based search functionality.

## Features

- **REST API** for data storage with tags
- **Tag-based search** - search data by one or multiple tags
- **Soft delete** - data is hidden, not permanently removed
- **Flat structure** - no hierarchy between data entries
- **Authentication** - user-based data ownership
- **H2 Database** - in-memory database for data storage

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Building the Application

```bash
mvn clean package
```

### Running the Application

```bash
java -jar target/data-storage-1.0.0.jar
```

The application will start on `http://localhost:8080`.

### Default Admin User

An admin user is created automatically with credentials configured in `application.properties`:
- Username: `hiipa` (configurable via `hiip.admin.username`)
- Password: `hiipa` (configurable via `hiip.admin.password`)
- Email: `admin@example.com` (configurable via `hiip.admin.email`)

Default values are used if not specified in configuration.

**Admin Reset on Startup**: By default, the admin user credentials are reset on every application startup to prevent being locked out of the application (`hiip.admin.reset-on-startup=true`). This ensures that even if the admin password is changed or the admin user is deactivated, you can always regain access by restarting the application. Set this to `false` in production if you want to preserve admin user changes across restarts.

## API Endpoints

All endpoints require JWT authentication (except authentication endpoints). 

### Authentication

The API uses JWT (JSON Web Token) for authentication. First obtain a token by logging in, then include it in the Authorization header for subsequent requests.

#### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "hiipa",
  "password": "hiipa"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "hiipa"
}
```

Example:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hiipa","password":"hiipa"}'
```

#### Refresh Token

```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token-here"
}
```

#### Logout

```bash
POST /api/v1/auth/logout
Authorization: Bearer <your-jwt-token>
```

#### Using JWT Token

Include the JWT token in the Authorization header for all authenticated requests:

```bash
Authorization: Bearer <your-jwt-token>
``` 

### API Versioning

The API uses URL-based versioning to ensure backward compatibility. The current version is `v1` and all endpoints are prefixed with `/api/v1/`. This allows for future API evolution while maintaining support for existing clients.

- Current API version: **v1**
- Base URL pattern: `http://localhost:8080/api/v1/`

### Create Data

```bash
POST /api/v1/data
Content-Type: application/json

{
  "content": "Your data content",
  "tags": ["tag1", "tag2"]
}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X POST http://localhost:8080/api/v1/data \
  -H "Content-Type: application/json" \
  -d '{"content":"My data","tags":["important","work"]}'
```

### Get All Data

```bash
GET /api/v1/data
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" http://localhost:8080/api/v1/data
```

### Get Data by ID

```bash
GET /api/v1/data/{id}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" http://localhost:8080/api/v1/data/1
```

### Search Data by Tags

```bash
GET /api/v1/data/search?tags=tag1&tags=tag2
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" "http://localhost:8080/api/v1/data/search?tags=important"
```

### Update Data

```bash
PUT /api/v1/data/{id}
Content-Type: application/json

{
  "content": "Updated content",
  "tags": ["updated", "tags"]
}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X PUT http://localhost:8080/api/v1/data/1 \
  -H "Content-Type: application/json" \
  -d '{"content":"Updated data","tags":["updated"]}'
```

### Hide Data (Soft Delete)

```bash
DELETE /api/v1/data/{id}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X DELETE http://localhost:8080/api/v1/data/1
```

## User Management API

**Note: All user management endpoints require admin privileges. Only users with `isAdmin=true` can access these endpoints.**

### Get All Users

```bash
GET /api/v1/users
```

Optional parameter: `includeInactive=true` to include deactivated users.

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" "http://localhost:8080/api/v1/users"
curl -H "Authorization: Bearer <your-jwt-token>" "http://localhost:8080/api/v1/users?includeInactive=true"
```

### Get User by ID

```bash
GET /api/users/{id}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" http://localhost:8080/api/users/1
```

### Get User by Username

```bash
GET /api/v1/users/username/{username}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" http://localhost:8080/api/v1/users/username/hiipa
```

### Create User

```bash
POST /api/v1/users
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "isAdmin": false,
  "isActive": true
}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123","email":"user@example.com","isAdmin":false}'
```

### Update User

```bash
PUT /api/v1/users/{id}
Content-Type: application/json

{
  "username": "updateduser",
  "password": "newpassword",
  "email": "updated@example.com",
  "isAdmin": true,
  "isActive": true
}
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X PUT http://localhost:8080/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"updateduser","email":"updated@example.com","isAdmin":true}'
```

### Delete User (Soft Delete)

```bash
DELETE /api/v1/users/{id}
```

This sets the user's `isActive` flag to `false` instead of permanently deleting the user.

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X DELETE http://localhost:8080/api/v1/users/1
```

### Activate User

```bash
PATCH /api/v1/users/{id}/activate
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X PATCH http://localhost:8080/api/v1/users/1/activate
```

### Deactivate User

```bash
PATCH /api/v1/users/{id}/deactivate
```

Example:
```bash
curl -H "Authorization: Bearer <your-jwt-token>" -X PATCH http://localhost:8080/api/v1/users/1/deactivate
```

## H2 Console

The H2 database console is available for development and testing:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:hiipdb`
- Username: `sa`
- Password: (leave empty)

## Configuration

The application can be configured via `application.properties` or environment variables. The following properties support environment variable overrides:

### Database Configuration
- `HIIP_DATASOURCE_URL` - Database URL (default: `jdbc:h2:mem:hiipdb`)
- `HIIP_DATASOURCE_DRIVER` - Database driver (default: `org.h2.Driver`)
- `HIIP_DATASOURCE_USERNAME` - Database username (default: `sa`)
- `HIIP_DATASOURCE_PASSWORD` - Database password (default: empty)
- `HIIP_HIBERNATE_DIALECT` - Setting the Hibernate dialect (default: `org.hibernate.dialect.H2Dialect`)

### H2 Console
- `HIIP_H2_CONSOLE_ENABLED` - Enable/disable H2 console (default: `false` - disabled for security)

### Admin User
- `HIIP_ADMIN_USERNAME` - Admin username (default: `hiipa`)
- `HIIP_ADMIN_PASSWORD` - Admin password (default: `hiipa`)
- `HIIP_ADMIN_EMAIL` - Admin email (default: `admin@example.com`)
- `HIIP_ADMIN_RESET_ON_STARTUP` - Reset admin user credentials on every startup (default: `true`)

### JWT Configuration
- `HIIP_JWT_SECRET` - JWT signing secret key (**REQUIRED** - no default for security)
- `HIIP_JWT_EXPIRATION` - JWT token expiration time in milliseconds (default: `86400000` - 24 hours)
- `HIIP_JWT_REFRESH_EXPIRATION` - Refresh token expiration time in milliseconds (default: `604800000` - 7 days)

### JWT Secret Generation

**Generate a secure JWT secret:**
```bash
# Generate a new JWT secret
./scripts/generate-jwt-secret.sh

# Use the generated secret
export HIIP_JWT_SECRET="your-generated-secret-here"
```

**⚠️ Security Note**: The JWT secret is now **required** and has no default value. You must generate and set a secure secret before starting the application.

### Example with Environment Variables

```bash
# Generate JWT secret first
./scripts/generate-jwt-secret.sh

# Set environment variables
export HIIP_ADMIN_USERNAME=myadmin
export HIIP_ADMIN_PASSWORD=mysecretpassword
export HIIP_ADMIN_RESET_ON_STARTUP=false
export HIIP_H2_CONSOLE_ENABLED=true  # Enable for development
export HIIP_JWT_SECRET="your-generated-secret-from-script"
export HIIP_JWT_EXPIRATION=3600000

# Start application
java -jar target/data-storage-1.0.0.jar
```

## Container Deployment

A `Containerfile` is provided for containerized deployment using Podman or Docker.

### Building the Container Image

```bash
# Build the application first
mvn clean package

# Build the container image
podman build -t hiip .
```

### Running with Podman/Docker

```bash
# Generate JWT secret first
JWT_SECRET=$(./scripts/generate-jwt-secret.sh | grep "HIIP_JWT_SECRET=" | cut -d'=' -f2)

# Run with minimal configuration
podman run -p 8080:8080 \
  -e HIIP_JWT_SECRET="$JWT_SECRET" \
  hiip

# Run with custom environment variables
podman run -p 8080:8080 \
  -e HIIP_ADMIN_USERNAME=myadmin \
  -e HIIP_ADMIN_PASSWORD=mysecretpassword \
  -e HIIP_ADMIN_RESET_ON_STARTUP=false \
  -e HIIP_H2_CONSOLE_ENABLED=false \
  -e HIIP_JWT_SECRET="$JWT_SECRET" \
  hiip

# Alternative: Auto-generate JWT secret on startup (NOT recommended for production)
podman run -p 8080:8080 \
  -e GENERATE_JWT_SECRET_ON_START=true \
  hiip
```

### Startup Script Options

The container includes a startup script with several options:

```bash
# Normal startup (requires HIIP_JWT_SECRET to be set)
./scripts/start.sh

# Generate a new JWT secret
./scripts/start.sh generate-secret

# Validate existing JWT secret
./scripts/start.sh validate-secret

# Show help
./scripts/start.sh help
```
```

## Technical Details

- **Framework**: Spring Boot 3.1.5
- **Database**: H2 (in-memory)
- **Security**: Spring Security with HTTP Basic Authentication
- **ORM**: Hibernate/JPA
- **Build Tool**: Maven

## License

Licensed under the Apache License, Version 2.0.

