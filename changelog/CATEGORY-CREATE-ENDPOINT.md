# Category Creation Endpoint

## Summary
Added an explicit endpoint for creating categories through the REST API, along with supporting DTO and service method. Includes permission checks to ensure users can only create child categories under categories they own or have write access to.

## Changes Made

### 1. New DTO: `CategoryRequest.java`
- **Location**: `src/main/java/com/hiip/datastorage/dto/CategoryRequest.java`
- **Purpose**: Request DTO for creating categories
- **Fields**:
  - `name`: Category name (required)
  - `path`: Complete category path (optional, can be auto-generated)
  - `parentId`: ID of parent category (optional, null for root categories)
  - `isGlobal`: Whether category should be global (default: false)

### 2. Service Method: `CategoryService.createCategory()`
- **Location**: `src/main/java/com/hiip/datastorage/service/CategoryService.java`
- **Signature**: `createCategory(String name, String path, Long parentId, String createdBy, boolean isGlobal)`
- **Behavior**:
  - Creates a single category (not the full hierarchy like `getOrCreateCategory`)
  - Auto-generates path from name and parent if path is not provided
  - Validates that category name is not empty
  - Validates that parent exists if parentId is provided
  - **Permission Check**: Verifies user owns or has write access to parent category
  - Checks for duplicate paths and throws exception if exists
  - Returns the created category entity
- **Permission Logic**:
  - User can create root categories (no parent) without restrictions
  - User can create child categories only if they:
    - Own the parent category (created it), OR
    - Have write access to the parent category (shared with write permission)

### 3. Controller Endpoint: `POST /api/v1/categories`
- **Location**: `src/main/java/com/hiip/datastorage/controller/CategoryController.java`
- **Method**: `createCategory(@RequestBody CategoryRequest, Authentication)`
- **Security**: Requires authentication (Bearer token)
- **Request Body**:
  ```json
  {
    "name": "Category Name",
    "path": "optional/path/to/category",
    "parentId": 123,
    "isGlobal": false
  }
  ```
- **Responses**:
  - `201 Created`: Category created successfully (returns CategoryResponse)
  - `400 Bad Request`: Invalid request or category already exists
  - `401 Unauthorized`: Missing or invalid authentication token
  - `403 Forbidden`: User lacks permission to create child under parent category
- **Features**:
  - Authenticated user is automatically set as the category owner
  - Path can be omitted and will be auto-generated from name and parent
  - Supports creating both root categories (no parentId) and child categories
  - Enforces permission checks on parent categories

## Usage Examples

### Create a Root Category
```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Electronics",
    "isGlobal": false
  }'
```
**Note**: Any authenticated user can create root categories.

### Create a Child Category with Parent
```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptops",
    "parentId": 1,
    "isGlobal": false
  }'
```
**Note**: User must own category ID 1 or have write access to it.

### Create a Category with Explicit Path
```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptops",
    "path": "electronics/computers/laptops/gaming",
    "isGlobal": false
  }'
```

### Permission Error Example
If you try to create a child category under a parent you don't have access to:
```bash
# Response: 403 Forbidden
{
  "message": "You don't have permission to create a child category under 'electronics/computers'. You must be the owner or have write access to the parent category."
}
```

## Differences from `getOrCreateCategory`

- **getOrCreateCategory**: Automatically creates all parent categories in the hierarchy if they don't exist
- **createCategory**: Only creates the specified category; parent must already exist if parentId is provided; enforces permission checks on parent

## Security Features

1. **Ownership Verification**: Users can only create child categories under categories they own
2. **Shared Access Verification**: Users with write permission on a shared category can create children
3. **Clear Error Messages**: Provides specific feedback when permission is denied
4. **Proper HTTP Status Codes**: Returns 403 Forbidden for permission issues, distinguishing from 400 Bad Request

## Permission Rules

| Scenario | Can Create? | Notes |
|----------|-------------|-------|
| Root category (no parent) | ✅ Yes | Any authenticated user |
| Child of owned category | ✅ Yes | User created the parent |
| Child of shared category (write access) | ✅ Yes | Parent shared with `canWrite=true` |
| Child of shared category (read-only) | ❌ No | Returns 403 Forbidden |
| Child of unowned/unshared category | ❌ No | Returns 403 Forbidden |
| Child of global category | ❌ No | Global categories cannot be modified |

## API Documentation

The endpoint is documented with OpenAPI/Swagger annotations and will appear in the Swagger UI at `/swagger-ui.html`.

## Testing

Build verification completed successfully:
```
mvn clean compile
[INFO] BUILD SUCCESS
```

## Date
October 25, 2025
