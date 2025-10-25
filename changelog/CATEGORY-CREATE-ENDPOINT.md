# Category Creation Endpoint

## Summary
Added an explicit endpoint for creating categories through the REST API, along with supporting DTO and service method.

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
  - Checks for duplicate paths and throws exception if exists
  - Returns the created category entity

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
- **Features**:
  - Authenticated user is automatically set as the category owner
  - Path can be omitted and will be auto-generated from name and parent
  - Supports creating both root categories (no parentId) and child categories

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

## Differences from `getOrCreateCategory`

- **getOrCreateCategory**: Automatically creates all parent categories in the hierarchy if they don't exist
- **createCategory**: Only creates the specified category; parent must already exist if parentId is provided

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
