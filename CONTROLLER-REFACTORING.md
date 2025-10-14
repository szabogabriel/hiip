# Controller Architecture Refactoring

## Overview
This document describes the refactoring of the controller architecture to implement a facade service layer pattern, separating HTTP concerns from business logic.

## Architecture Pattern

### Before Refactoring
Controllers directly injected multiple business services:
```java
@RestController
public class AuthController {
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private AccountLockoutService accountLockoutService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private EmailService emailService;
    @Autowired private UserService userService;
    
    // Complex business logic mixed with HTTP handling
}
```

### After Refactoring
Controllers now use dedicated facade services:
```java
@RestController
public class AuthController {
    @Autowired private AuthFacadeService authFacadeService;
    
    // Clean HTTP handling only
}
```

## Benefits

### 1. **Separation of Concerns**
- **Controllers**: Handle HTTP requests/responses, parameter validation, status codes
- **Facade Services**: Orchestrate business logic, coordinate multiple services
- **Business Services**: Implement specific domain logic

### 2. **Improved Testability**
- Controllers can be tested with a single mock (facade service)
- Business logic can be tested independently
- Easier to write unit tests

### 3. **Better Maintainability**
- Changes to business logic don't affect controller structure
- Clearer responsibility boundaries
- Easier to understand and navigate code

### 4. **Reusability**
- Facade services can be reused by other components (e.g., batch jobs, scheduled tasks)
- Business logic is decoupled from HTTP layer

### 5. **Enhanced Documentation**
- Controllers focus on API documentation (Swagger annotations)
- Facade services document business workflows

## Refactored Components

### 1. AuthController → AuthFacadeService
**Responsibilities of AuthFacadeService:**
- Authenticate users with account lockout handling
- Refresh JWT tokens
- Manage password reset workflow
- Coordinate between: AuthenticationManager, JwtUtil, UserDetailsService, AccountLockoutService, PasswordResetService, EmailService, UserService

**API Endpoints:**
- `POST /api/v1/auth/login` - User authentication
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/auth/password-reset/request` - Request password reset
- `POST /api/v1/auth/password-reset/confirm` - Confirm password reset
- `GET /api/v1/auth/password-reset/validate` - Validate reset token

### 2. DataStorageController → DataStorageFacadeService
**Responsibilities of DataStorageFacadeService:**
- CRUD operations for data storage
- DTO to Entity conversions
- Logging and monitoring
- Coordinate with DataStorageService

**API Endpoints:**
- `POST /api/v1/data` - Create data
- `GET /api/v1/data/{id}` - Get data by ID
- `GET /api/v1/data` - Get all data
- `GET /api/v1/data/search` - Search by tags
- `PUT /api/v1/data/{id}` - Update data
- `DELETE /api/v1/data/{id}` - Delete data

### 3. UserController → UserFacadeService
**Responsibilities of UserFacadeService:**
- User management operations
- Admin privilege checks
- Validation logic
- Coordinate with UserService

**API Endpoints:**
- `GET /api/v1/users` - Get all users
- `GET /api/v1/users/{id}` - Get user by ID
- `GET /api/v1/users/username/{username}` - Get user by username
- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user
- `PATCH /api/v1/users/{id}/activate` - Activate user
- `PATCH /api/v1/users/{id}/deactivate` - Deactivate user

## Implementation Details

### Response Pattern
Facade services return `Map<String, Object>` containing:
- `status`: Operation status (SUCCESS, ERROR, FORBIDDEN, NOT_FOUND, etc.)
- `httpStatus`: HTTP status code (200, 400, 403, 404, etc.)
- `data`: Result data (user, jwtResponse, etc.)
- `error`: Error message if applicable

Example:
```java
Map<String, Object> result = authFacadeService.authenticateUser(username, password);
int httpStatus = (int) result.get("httpStatus");

if ("SUCCESS".equals(result.get("status"))) {
    return ResponseEntity.ok(result.get("jwtResponse"));
}

return ResponseEntity.status(httpStatus).body(result.get("error"));
```

### Logging Strategy
- **Controllers**: Log incoming requests and HTTP-level issues
- **Facade Services**: Log business operations and workflows
- **Business Services**: Log domain-specific operations

### Error Handling
- Validation errors handled in facade services
- Business exceptions caught and translated to appropriate HTTP responses
- Consistent error response format across all endpoints

## File Structure

```
src/main/java/com/hiip/datastorage/
├── controller/
│   ├── AuthController.java              # HTTP handling only
│   ├── DataStorageController.java       # HTTP handling only
│   └── UserController.java              # HTTP handling only
├── service/
│   ├── AuthFacadeService.java           # NEW: Auth orchestration
│   ├── DataStorageFacadeService.java    # NEW: Data storage orchestration
│   ├── UserFacadeService.java           # NEW: User management orchestration
│   ├── UserService.java                 # Existing: User business logic
│   ├── DataStorageService.java          # Existing: Data storage business logic
│   ├── AccountLockoutService.java       # Existing: Account lockout logic
│   ├── PasswordResetService.java        # Existing: Password reset logic
│   ├── EmailService.java                # Existing: Email operations
│   └── CustomUserDetailsService.java    # Existing: User details loading
```

## Migration Guide

### For Future Endpoints

When creating new controllers:

1. **Create a facade service** for the controller
2. **Controller responsibilities:**
   - Define HTTP endpoints with proper annotations
   - Extract parameters from requests
   - Call facade service methods
   - Convert facade service results to HTTP responses
   - Handle HTTP status codes

3. **Facade service responsibilities:**
   - Validate business rules
   - Coordinate multiple business services
   - Handle transactions if needed
   - Return structured results with status information

4. **Example:**

```java
// Controller
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    @Autowired private ProductFacadeService productFacadeService;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Map<String, Object> result = productFacadeService.getProduct(id);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("product"));
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }
}

// Facade Service
@Service
public class ProductFacadeService {
    @Autowired private ProductService productService;
    @Autowired private InventoryService inventoryService;
    
    public Map<String, Object> getProduct(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Product> product = productService.findById(id);
        if (!product.isPresent()) {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
            return result;
        }
        
        // Enrich with inventory data
        Integer stock = inventoryService.getStock(id);
        ProductDTO dto = new ProductDTO(product.get(), stock);
        
        result.put("status", "SUCCESS");
        result.put("product", dto);
        result.put("httpStatus", 200);
        return result;
    }
}
```

## Testing Strategy

### Controller Tests
Mock the facade service and verify:
- Correct HTTP status codes
- Proper request parameter extraction
- Correct response body structure

### Facade Service Tests
Mock business services and verify:
- Correct service orchestration
- Business rule enforcement
- Proper error handling
- Transaction management

### Business Service Tests
Test domain logic in isolation

## Performance Considerations

- **No Performance Impact**: Facade layer adds negligible overhead
- **Improved Caching**: Business logic can be cached at facade level
- **Better Monitoring**: Clear points for performance monitoring

## Swagger Documentation

All controllers now have comprehensive Swagger annotations:
- Operation descriptions
- Response codes with descriptions
- Request/response schemas
- Security requirements

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

## Backward Compatibility

✅ **All existing API endpoints remain unchanged**
✅ **Request/response formats unchanged**
✅ **Authentication and authorization unchanged**
✅ **Database schema unchanged**

The refactoring is purely internal - no breaking changes for API consumers.

## Next Steps

1. **Add integration tests** for facade services
2. **Consider adding @Transactional** annotations where appropriate
3. **Implement caching** at facade service level for frequently accessed data
4. **Add metrics/monitoring** at facade service layer
5. **Consider adding rate limiting** at facade service level

## Summary

This refactoring improves code quality by:
- ✅ Separating HTTP concerns from business logic
- ✅ Making controllers thin and focused
- ✅ Improving testability and maintainability
- ✅ Enabling better reusability of business logic
- ✅ Following industry best practices
- ✅ Making the codebase easier to understand and navigate

The application now follows a clean 3-layer architecture:
1. **Presentation Layer** (Controllers): HTTP handling
2. **Service Facade Layer** (Facade Services): Business orchestration
3. **Business Logic Layer** (Business Services): Domain logic
