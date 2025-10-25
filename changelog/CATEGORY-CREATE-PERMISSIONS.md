# Category Creation Permission Checks

## Overview
Added permission validation to the category creation endpoint to ensure users can only create child categories under categories they own or have write access to.

## Implementation Details

### Service Layer (`CategoryService.createCategory()`)

When a `parentId` is provided, the service now:
1. Fetches the parent category from the database
2. Calls `parent.hasWritePermission(createdBy)` to verify permissions
3. Throws `IllegalArgumentException` with descriptive message if permission denied

```java
// Check if user has write permission on the parent category
if (!parent.hasWritePermission(createdBy)) {
    throw new IllegalArgumentException(
        "You don't have permission to create a child category under '" + parent.getPath() + "'. " +
        "You must be the owner or have write access to the parent category."
    );
}
```

### Permission Logic (from `Category.hasWritePermission()`)

The permission check evaluates three conditions:
1. **Global Categories**: Returns `false` (global categories cannot be modified)
2. **Ownership**: Returns `true` if `createdBy` matches the username
3. **Shared Access**: Returns `true` if category is shared with user and `canWrite=true`

### Controller Layer (`CategoryController.createCategory()`)

The controller distinguishes between permission errors and validation errors:
- Permission errors (message contains "don't have permission"): Returns **403 Forbidden**
- Other validation errors: Returns **400 Bad Request**

```java
catch (IllegalArgumentException e) {
    // Check if it's a permission error
    if (e.getMessage().contains("don't have permission")) {
        return ResponseEntity.status(403).body(e.getMessage());
    }
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

## Updated API Documentation

Added `403` response code to the OpenAPI documentation:
```java
@ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions on parent category")
```

## Test Scenarios

### Scenario 1: Create Root Category
- **Input**: No parentId
- **Expected**: ✅ Success (any authenticated user)
- **Status**: 201 Created

### Scenario 2: Create Child of Owned Category
- **Input**: parentId = category owned by current user
- **Expected**: ✅ Success
- **Status**: 201 Created

### Scenario 3: Create Child of Shared Category (Write Access)
- **Setup**: Parent category shared with user, `canWrite=true`
- **Expected**: ✅ Success
- **Status**: 201 Created

### Scenario 4: Create Child of Shared Category (Read-Only)
- **Setup**: Parent category shared with user, `canWrite=false`
- **Expected**: ❌ Denied
- **Status**: 403 Forbidden
- **Message**: "You don't have permission to create a child category under '...'..."

### Scenario 5: Create Child of Unowned Category
- **Setup**: Parent category not owned or shared with user
- **Expected**: ❌ Denied
- **Status**: 403 Forbidden
- **Message**: "You don't have permission to create a child category under '...'..."

### Scenario 6: Create Child of Global Category
- **Setup**: Parent category has `isGlobal=true`
- **Expected**: ❌ Denied
- **Status**: 403 Forbidden
- **Message**: "You don't have permission to create a child category under '...'..."

## Security Benefits

1. **Prevents Unauthorized Hierarchy Manipulation**: Users cannot inject categories into hierarchies they don't control
2. **Respects Sharing Model**: Honors the existing category sharing and permission system
3. **Clear Error Messages**: Helps users understand why their request was denied
4. **Proper HTTP Semantics**: Uses 403 Forbidden for authorization issues vs 400 Bad Request for validation

## Related Code

- `Category.hasWritePermission()`: `/src/main/java/com/hiip/datastorage/entity/Category.java`
- `CategoryService.createCategory()`: `/src/main/java/com/hiip/datastorage/service/CategoryService.java`
- `CategoryController.createCategory()`: `/src/main/java/com/hiip/datastorage/controller/CategoryController.java`

## Date
October 25, 2025
