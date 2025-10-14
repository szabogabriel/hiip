# Category Sharing and Permissions Feature

## Overview
This document describes the hybrid category ownership model with sharing capabilities and permission management implemented in the HIIP Data Storage application.

## Architecture Model

### Hybrid Approach
Categories in the system support three access levels:
1. **Global Categories** - Accessible to all users (created by admins)
2. **Private Categories** - Owned by individual users
3. **Shared Categories** - Private categories shared with specific users with granular permissions

### Key Features
- ✅ User ownership tracking (`createdBy` field)
- ✅ Global category flag for system-wide categories
- ✅ Sharing mechanism with read/write permissions
- ✅ Privacy by default (categories are private unless marked global or explicitly shared)
- ✅ Permission-based access control

## Database Schema

### Categories Table
```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(500) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id),
    created_by VARCHAR(100),           -- NEW: Username of creator
    is_global BOOLEAN DEFAULT false,   -- NEW: Global category flag
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Category Shares Table (NEW)
```sql
CREATE TABLE category_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    shared_with_username VARCHAR(100) NOT NULL,
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    shared_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (category_id, shared_with_username)
);
```

## Entity Model

### Category Entity
```java
@Entity
public class Category {
    private Long id;
    private String name;
    private String path;
    private Category parent;
    private List<Category> children;
    
    // NEW FIELDS
    private String createdBy;              // Username of creator
    private boolean isGlobal;              // Global category flag
    private List<CategoryShare> sharedWith; // Sharing relationships
    
    // Helper methods
    public boolean isAccessibleBy(String username);
    public boolean hasWritePermission(String username);
    public void shareWith(String username, boolean canRead, boolean canWrite);
    public void unshareWith(String username);
}
```

### CategoryShare Entity (NEW)
```java
@Entity
public class CategoryShare {
    private Long id;
    private Category category;
    private String sharedWithUsername;
    private boolean canRead;               // Read permission
    private boolean canWrite;              // Write permission
    private LocalDateTime sharedAt;
    private LocalDateTime updatedAt;
}
```

## Access Control Logic

### Category Accessibility
A category is **accessible** to a user if:
1. The category is **global** (`isGlobal = true`), OR
2. The user **created** the category (`createdBy = username`), OR
3. The category is **shared** with the user with `canRead = true`

### Category Write Permission
A user has **write permission** on a category if:
1. The user **created** the category (`createdBy = username`), OR
2. The category is **shared** with the user with `canWrite = true`

**Note:** Global categories cannot be modified by regular users (admin-only).

## API Endpoints

### Category Management (Updated)

#### 1. Get All Categories (Updated)
```http
GET /api/v1/categories
Authorization: Bearer <token>
```
Returns all categories **accessible** by the current user (global, owned, or shared).

**Response:**
```json
[
  {
    "id": 1,
    "name": "electronics",
    "path": "electronics",
    "parentId": null,
    "createdBy": "john_doe",
    "isGlobal": false,
    "sharedWith": [
      {
        "id": 1,
        "sharedWithUsername": "jane_smith",
        "canRead": true,
        "canWrite": false,
        "sharedAt": "2025-10-14T10:00:00"
      }
    ],
    "createdAt": "2025-10-14T10:00:00",
    "updatedAt": "2025-10-14T10:00:00"
  }
]
```

#### 2. Get My Categories (NEW)
```http
GET /api/v1/categories/my-categories
Authorization: Bearer <token>
```
Returns categories accessible by the current user (global, owned, or shared).

### Category Sharing (NEW)

#### 3. Share Category
```http
POST /api/v1/categories/{id}/share
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "jane_smith",
  "canRead": true,
  "canWrite": false
}
```

**Permissions Required:** User must own the category (be the creator)

**Response:**
```json
{
  "id": 1,
  "sharedWithUsername": "jane_smith",
  "canRead": true,
  "canWrite": false,
  "sharedAt": "2025-10-14T10:00:00"
}
```

**Permission Levels:**
- `canRead: true, canWrite: false` - Read-only access
- `canRead: true, canWrite: true` - Full access (can create/update data in category)
- `canRead: false, canWrite: false` - No access (revoke access)

#### 4. Unshare Category
```http
DELETE /api/v1/categories/{id}/share/{username}
Authorization: Bearer <token>
```

**Permissions Required:** User must own the category

**Response:** 200 OK (empty body)

#### 5. Get Category Shares
```http
GET /api/v1/categories/{id}/shares
Authorization: Bearer <token>
```

**Permissions Required:** User must own the category

**Response:**
```json
[
  {
    "id": 1,
    "sharedWithUsername": "jane_smith",
    "canRead": true,
    "canWrite": false,
    "sharedAt": "2025-10-14T10:00:00"
  },
  {
    "id": 2,
    "sharedWithUsername": "bob_jones",
    "canRead": true,
    "canWrite": true,
    "sharedAt": "2025-10-14T11:00:00"
  }
]
```

## Usage Examples

### Example 1: Create Private Category
```bash
# User john_doe creates data with category
curl -X POST http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer ${JOHN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {"product": "Laptop"},
    "tags": ["electronics"],
    "category": "electronics/computers/laptops"
  }'
```

**Result:** Category created with `createdBy = "john_doe"`, `isGlobal = false`

### Example 2: Share Category (Read-Only)
```bash
# John shares category with Jane (read-only)
curl -X POST http://localhost:8080/api/v1/categories/1/share \
  -H "Authorization: Bearer ${JOHN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_smith",
    "canRead": true,
    "canWrite": false
  }'
```

**Result:** Jane can now see categories and data in "electronics/computers/laptops" but cannot create/modify

### Example 3: Share Category (Read-Write)
```bash
# John shares category with Bob (full access)
curl -X POST http://localhost:8080/api/v1/categories/1/share \
  -H "Authorization: Bearer ${JOHN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob_jones",
    "canRead": true,
    "canWrite": true
  }'
```

**Result:** Bob can see AND create/modify data in the category

### Example 4: View Accessible Categories
```bash
# Jane views her accessible categories
curl -X GET http://localhost:8080/api/v1/categories/my-categories \
  -H "Authorization: Bearer ${JANE_TOKEN}"
```

**Result:** Returns categories Jane owns + global categories + categories shared with Jane

### Example 5: Unshare Category
```bash
# John removes Jane's access
curl -X DELETE http://localhost:8080/api/v1/categories/1/share/jane_smith \
  -H "Authorization: Bearer ${JOHN_TOKEN}"
```

**Result:** Jane can no longer access the category

## Permission Matrix

| Scenario | Can View Category | Can Create Data | Can Update Data | Can Share Category |
|----------|------------------|-----------------|-----------------|-------------------|
| Global Category | ✅ All Users | ❌ No | ❌ No | ❌ Admin Only |
| Category Owner | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Shared (Read) | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Shared (Write) | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| Not Shared | ❌ No | ❌ No | ❌ No | ❌ No |

## Use Cases

### Use Case 1: Personal Organization
- **User:** Creates personal categories for organizing their own data
- **Privacy:** Categories are private by default
- **Benefit:** Complete control over personal organization

### Use Case 2: Team Collaboration
- **Team Lead:** Creates project category structure
- **Sharing:** Shares with team members (read or read-write)
- **Collaboration:** Team can view or contribute to shared categories
- **Control:** Team lead can revoke access anytime

### Use Case 3: Department-Wide Standards
- **Admin:** Creates global categories for company-wide use
- **Access:** All users can see and use global categories
- **Control:** Only admins can modify global categories

### Use Case 4: Client Projects
- **Consultant:** Creates categories for each client
- **Selective Sharing:** Shares specific client categories with team members
- **Privacy:** Other clients' categories remain private

## Implementation Details

### Automatic Category Creation (Updated)
When creating data with a category path:
```java
Category category = categoryService.getOrCreateCategory(
    "electronics/computers/laptops", 
    currentUsername  // NEW: username parameter
);
```

- Category is created with `createdBy = currentUsername`
- Category is private by default (`isGlobal = false`)
- Parent categories inherit same ownership

### Service Methods (NEW)

```java
// CategoryService
List<Category> getCategoriesAccessibleByUser(String username);
List<Category> getCategoriesWritableByUser(String username);
CategoryShare shareCategory(Long categoryId, String username, boolean canRead, boolean canWrite);
void unshareCategory(Long categoryId, String username);
List<CategoryShare> getCategoryShares(Long categoryId);
boolean canUserAccessCategory(Long categoryId, String username);
boolean canUserWriteCategory(Long categoryId, String username);
```

### Repository Queries (NEW)

```java
// CategoryRepository
@Query("SELECT DISTINCT c FROM Category c LEFT JOIN c.sharedWith s " +
       "WHERE c.isGlobal = true OR c.createdBy = :username OR " +
       "(s.sharedWithUsername = :username AND s.canRead = true)")
List<Category> findAccessibleByUser(String username);

@Query("SELECT DISTINCT c FROM Category c LEFT JOIN c.sharedWith s " +
       "WHERE c.createdBy = :username OR " +
       "(s.sharedWithUsername = :username AND s.canWrite = true)")
List<Category> findWritableByUser(String username);
```

## Security Considerations

### 1. Ownership Verification
All sharing operations verify that the requesting user owns the category:
```java
if (!categoryService.canUserWriteCategory(id, currentUser)) {
    return ResponseEntity.status(403).body("Permission denied");
}
```

### 2. Cascading Permissions
- Sharing a parent category does NOT automatically share child categories
- Each category requires explicit sharing
- This provides fine-grained control

### 3. Permission Inheritance
- Child data entries inherit category permissions
- Users with read permission on category can view its data
- Users with write permission on category can create/modify its data

### 4. Global Category Protection
- Global categories can only be created/modified by admins
- Regular users cannot modify global categories
- Prevents accidental system-wide changes

## Migration Path

### Existing Data
- Existing categories without `createdBy` will have `createdBy = null`
- These can be treated as "legacy" categories
- Admin can assign ownership or make them global
- No data loss - fully backward compatible

### Database Migration
H2 will auto-create new columns on startup. For production:
```sql
-- Add new columns to categories
ALTER TABLE categories 
ADD COLUMN created_by VARCHAR(100),
ADD COLUMN is_global BOOLEAN DEFAULT false;

-- Create category_shares table
CREATE TABLE category_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    shared_with_username VARCHAR(100) NOT NULL,
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    shared_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    UNIQUE (category_id, shared_with_username)
);

-- Create indexes
CREATE INDEX idx_category_created_by ON categories(created_by);
CREATE INDEX idx_category_is_global ON categories(is_global);
CREATE INDEX idx_category_share_username ON category_shares(shared_with_username);
CREATE INDEX idx_category_share_category ON category_shares(category_id);
```

## Testing Scenarios

### 1. Category Creation
- ✅ User creates category → ownership set
- ✅ Category path normalized correctly
- ✅ Parent categories inherit ownership

### 2. Sharing
- ✅ Owner can share with read-only
- ✅ Owner can share with read-write
- ✅ Non-owner cannot share
- ✅ Can update existing share

### 3. Access Control
- ✅ Owner can access own categories
- ✅ Shared user can access shared categories
- ✅ Non-shared user cannot access
- ✅ Global categories accessible to all

### 4. Permission Enforcement
- ✅ Read-only user cannot create data
- ✅ Write user can create data
- ✅ Owner has full control
- ✅ Sharing requires ownership

## Future Enhancements

1. **Group Sharing** - Share with user groups instead of individual users
2. **Role-Based Permissions** - Define roles (viewer, editor, admin)
3. **Sharing Expiration** - Time-limited access
4. **Sharing Notifications** - Notify users when categories are shared
5. **Audit Log** - Track all sharing/unsharing operations
6. **Bulk Sharing** - Share multiple categories at once
7. **Permission Templates** - Predefined permission sets
8. **Category Transfer** - Transfer ownership to another user

## Summary

The hybrid category model provides:
✅ **Privacy** - Categories are private by default  
✅ **Collaboration** - Selective sharing with permissions  
✅ **Control** - Granular read/write permissions  
✅ **Flexibility** - Global categories for common use  
✅ **Security** - Ownership verification on all operations  
✅ **Scalability** - Efficient queries with proper indexing  
✅ **Backward Compatible** - Works with existing data  

This implementation balances privacy, collaboration, and usability, providing a robust foundation for multi-user category management.
