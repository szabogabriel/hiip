# Swagger UI Implementation

## Overview
Added comprehensive OpenAPI 3.0 documentation with Swagger UI for interactive API exploration and testing.

## What Was Added

### 1. Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 2. OpenAPI Configuration (OpenApiConfig.java)
**Location:** `src/main/java/com/hiip/datastorage/config/OpenApiConfig.java`

**Features:**
- API metadata (title, description, version, contact, license)
- JWT Bearer authentication scheme
- Development and production server configuration
- Security requirements for all endpoints

### 3. Security Configuration Updates
**File:** `src/main/java/com/hiip/datastorage/config/SecurityConfig.java`

**Added public access to:**
- `/swagger-ui/**` - Swagger UI interface
- `/v3/api-docs/**` - OpenAPI JSON documentation
- `/swagger-ui.html` - Swagger UI entry point

### 4. Controller Annotations
**File:** `src/main/java/com/hiip/datastorage/controller/AuthController.java`

**Added:**
- `@Tag` for controller-level documentation
- `@Operation` for endpoint descriptions
- `@ApiResponses` for response documentation
- `@Schema` references for request/response bodies

### 5. Application Properties
**File:** `src/main/resources/application.properties`

**Configuration:**
```properties
# OpenAPI/Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
hiip.openapi.dev-url=${HIIP_OPENAPI_DEV_URL:http://localhost:8080}
hiip.openapi.prod-url=${HIIP_OPENAPI_PROD_URL:}
```

### 6. UI Integration
**File:** `src/main/resources/static/ui/index.html`

**Updates:**
- Added Swagger UI button in the UI
- Added API documentation endpoints section
- Added JavaScript function to open Swagger UI in new tab

## How to Access

### Swagger UI Interface
```
http://localhost:8080/swagger-ui.html
```
Interactive API documentation with "Try it out" functionality

### OpenAPI JSON
```
http://localhost:8080/v3/api-docs
```
Raw OpenAPI 3.0 specification in JSON format

### Main Application UI
```
http://localhost:8080/
```
Click the "📖 Swagger UI" button to open API documentation

## Features

### 🔐 JWT Authentication in Swagger
1. Click the "Authorize" button in Swagger UI
2. Enter your JWT token in the format: `Bearer <your-token>`
3. All subsequent API calls will include the authorization header

### 📖 Interactive Documentation
- **Try it Out**: Test API endpoints directly from the browser
- **Request/Response Examples**: View example payloads
- **Schema Documentation**: Explore data models and structures
- **Response Codes**: See all possible response codes with descriptions

### 🎯 Endpoint Organization
- **Authentication**: Login, refresh, logout, password reset
- **Data Storage**: CRUD operations for data storage
- **User Management**: User administration endpoints
- **Health & Monitoring**: Actuator endpoints

## API Documentation Standards

### Tags
Controllers are organized by functional areas:
- `Authentication` - User authentication and session management
- `data-storage-controller` - Data storage operations
- `user-controller` - User management

### Security
All authenticated endpoints show:
- 🔒 Lock icon indicating authentication required
- JWT Bearer token requirement
- 401 Unauthorized response documentation

### Response Documentation
Each endpoint includes:
- Success responses (200, 201, 204)
- Client error responses (400, 401, 403, 404, 423)
- Server error responses (500)
- Example payloads and schemas

## Usage Example

### 1. Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "hiipa", "password": "hiipa"}'
```

### 2. Use Token in Swagger UI
1. Open http://localhost:8080/swagger-ui.html
2. Click "Authorize" button
3. Enter: `Bearer <your-access-token>`
4. Click "Authorize"
5. Now you can test authenticated endpoints!

### 3. Test Endpoints
- Expand any endpoint
- Click "Try it out"
- Fill in parameters
- Click "Execute"
- View response

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `HIIP_OPENAPI_DEV_URL` | `http://localhost:8080` | Development server URL |
| `HIIP_OPENAPI_PROD_URL` | Empty | Production server URL |

## Customization

### Add More Servers
Edit `OpenApiConfig.java` and add additional servers:
```java
Server stagingServer = new Server()
    .url("https://staging.example.com")
    .description("Staging Server");
servers.add(stagingServer);
```

### Customize API Information
Edit the `Info` object in `OpenApiConfig.java`:
```java
.info(new Info()
    .title("Your API Title")
    .description("Your API Description")
    .version("2.0.0")
    .contact(new Contact()
        .name("Your Team")
        .email("support@example.com")))
```

### Add Endpoint Documentation
Add annotations to controller methods:
```java
@Operation(
    summary = "Brief description",
    description = "Detailed description"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Not found")
})
```

## Testing

All Swagger endpoints tested and verified:
- ✅ OpenAPI JSON generation
- ✅ Swagger UI page rendering
- ✅ JWT authentication integration
- ✅ Endpoint documentation
- ✅ Try it out functionality
- ✅ Security configuration

## Benefits

1. **Interactive Testing**: Test APIs without external tools
2. **Clear Documentation**: Auto-generated from code annotations
3. **Standards Compliant**: OpenAPI 3.0 specification
4. **Security Integration**: JWT authentication built-in
5. **Developer Friendly**: Easy to explore and understand APIs
6. **Client Generation**: Use OpenAPI spec to generate client SDKs

## Next Steps

To further enhance the API documentation:
1. Add more detailed descriptions to all endpoints
2. Add example values to request/response schemas
3. Document all possible error responses
4. Add operation IDs for better client generation
5. Include request/response examples
