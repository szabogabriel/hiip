# Category Structure Implementation

## Overview
This document describes the hierarchical category structure implementation for the HIIP Data Storage application.

## Features

### Category Entity
- **Hierarchical Structure**: Categories form a tree structure where each category can have a parent
- **Path Storage**: Complete path from root to leaf is stored (e.g., "electronics/computers/laptops")
- **Automatic Creation**: Categories are created implicitly when data is created with a category path
- **Unique Paths**: Each category path is unique across the system

### Database Schema

#### Category Table
```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(500) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### DataStorage Category Relationship
```sql
ALTER TABLE data_storage ADD COLUMN category_id BIGINT REFERENCES categories(id);
```

## API Endpoints

### Category Management

#### 1. Get All Categories
```
GET /api/v1/categories
Authorization: Bearer <token>
```

**Response**: List of all categories in flat structure
```json
[
  {
    "id": 1,
    "name": "electronics",
    "path": "electronics",
    "parentId": null,
    "createdAt": "2025-10-14T10:00:00",
    "updatedAt": "2025-10-14T10:00:00"
  },
  {
    "id": 2,
    "name": "computers",
    "path": "electronics/computers",
    "parentId": 1,
    "createdAt": "2025-10-14T10:00:00",
    "updatedAt": "2025-10-14T10:00:00"
  }
]
```

#### 2. Get Root Categories
```
GET /api/v1/categories/roots
Authorization: Bearer <token>
```

**Response**: List of root categories (categories without parents)

#### 3. Get Category Tree
```
GET /api/v1/categories/tree
Authorization: Bearer <token>
```

**Response**: Complete category hierarchy with nested children
```json
[
  {
    "id": 1,
    "name": "electronics",
    "path": "electronics",
    "parentId": null,
    "children": [
      {
        "id": 2,
        "name": "computers",
        "path": "electronics/computers",
        "parentId": 1,
        "children": [
          {
            "id": 3,
            "name": "laptops",
            "path": "electronics/computers/laptops",
            "parentId": 2,
            "children": []
          }
        ]
      }
    ]
  }
]
```

#### 4. Search Categories by Path Prefix
```
GET /api/v1/categories/search?prefix=electronics
Authorization: Bearer <token>
```

**Response**: All categories starting with the given prefix

#### 5. Get Category by ID
```
GET /api/v1/categories/{id}
Authorization: Bearer <token>
```

**Response**: Single category details

### Data Storage with Categories

#### Create Data with Category
```
POST /api/v1/data
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": {"message": "Hello World"},
  "tags": ["important", "test"],
  "category": "electronics/computers/laptops"
}
```

**Behavior**:
- If category path doesn't exist, all parent categories are created automatically
- Example: "electronics/computers/laptops" will create:
  1. "electronics" (if doesn't exist)
  2. "electronics/computers" (if doesn't exist)
  3. "electronics/computers/laptops" (if doesn't exist)

#### Update Data with Category
```
PUT /api/v1/data/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": {"message": "Updated content"},
  "tags": ["updated"],
  "category": "books/fiction/scifi"
}
```

#### Response with Category
```json
{
  "id": 1,
  "content": {"message": "Hello World"},
  "tags": ["important", "test"],
  "category": "electronics/computers/laptops",
  "owner": "john_doe",
  "createdAt": "2025-10-14T10:00:00",
  "updatedAt": "2025-10-14T10:00:00"
}
```

## Category Path Format

### Rules
1. **Separator**: Use forward slash `/` to separate category levels
2. **Normalization**: 
   - Leading/trailing slashes are removed
   - Multiple consecutive slashes are replaced with single slash
   - Whitespace is trimmed from each segment
3. **Examples**:
   - `"electronics/computers/laptops"` ✅ Valid
   - `"/electronics/computers/"` → Normalized to `"electronics/computers"` ✅
   - `"electronics//computers"` → Normalized to `"electronics/computers"` ✅
   - `"electronics / computers"` → Normalized to `"electronics/computers"` ✅

### Best Practices
- Use lowercase for consistency
- Use descriptive names
- Keep hierarchy depth reasonable (3-5 levels maximum)
- Examples of good category paths:
  - `"electronics/computers/laptops"`
  - `"books/fiction/scifi"`
  - `"products/clothing/mens/shirts"`
  - `"documents/reports/2025/q1"`

## Implementation Details

### CategoryService
**Key Methods**:
- `getOrCreateCategory(String categoryPath)`: Main method that creates category hierarchy
- `normalizePath(String path)`: Normalizes category paths
- `getAllCategories()`: Returns all categories
- `getRootCategories()`: Returns categories without parents
- `getCategoryByPath(String path)`: Finds category by exact path

### Automatic Category Creation Flow
1. User creates data with category path: `"electronics/computers/laptops"`
2. `DataStorageFacadeService.createData()` calls `CategoryService.getOrCreateCategory()`
3. CategoryService:
   - Normalizes the path
   - Splits into segments: `["electronics", "computers", "laptops"]`
   - Iterates through segments:
     - Checks if `"electronics"` exists → creates if not
     - Checks if `"electronics/computers"` exists → creates if not (with parent = "electronics")
     - Checks if `"electronics/computers/laptops"` exists → creates if not (with parent = "electronics/computers")
4. Returns the leaf category entity
5. DataStorage entity is created with reference to the category

### Database Relationships
```
Category (parent-child self-reference)
    ↑
    |
    | (Many-to-One)
    |
DataStorage
```

## UI Integration

### Create Data Form
- New field: **Category** input
- Placeholder: `"e.g., electronics/computers/laptops"`
- Help text: "Separate categories with slashes. They will be created automatically."
- Optional field (can be left empty)

### Data Display
- Category displayed with folder icon 📁
- Shows full path: `📁 electronics/computers/laptops`
- Styled with distinct background color

### Example UI Flow
1. User enters category: `"books/fiction/scifi"`
2. User fills content and tags
3. Submits form
4. Backend creates category hierarchy automatically
5. Data is created with category reference
6. UI displays data with category badge

## Testing the Feature

### Manual Testing Steps

1. **Create data with a new category**:
```bash
curl -X POST http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {"product": "Laptop", "price": 999},
    "tags": ["electronics", "sale"],
    "category": "electronics/computers/laptops"
  }'
```

2. **Verify category was created**:
```bash
curl -X GET http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer <token>"
```

3. **Get category tree**:
```bash
curl -X GET http://localhost:8080/api/v1/categories/tree \
  -H "Authorization: Bearer <token>"
```

4. **Create data with existing category**:
```bash
curl -X POST http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {"product": "Mouse", "price": 29},
    "tags": ["electronics", "accessories"],
    "category": "electronics/computers"
  }'
```

5. **Search categories**:
```bash
curl -X GET "http://localhost:8080/api/v1/categories/search?prefix=electronics" \
  -H "Authorization: Bearer <token>"
```

### Expected Behavior

1. **First Request** (`electronics/computers/laptops`):
   - Creates 3 categories:
     - `electronics` (id: 1, parent: null)
     - `electronics/computers` (id: 2, parent: 1)
     - `electronics/computers/laptops` (id: 3, parent: 2)

2. **Second Request** (`electronics/computers`):
   - No new categories created (already exists)
   - Data references existing category (id: 2)

3. **Category Tree**:
```json
[
  {
    "id": 1,
    "name": "electronics",
    "path": "electronics",
    "children": [
      {
        "id": 2,
        "name": "computers",
        "path": "electronics/computers",
        "children": [
          {
            "id": 3,
            "name": "laptops",
            "path": "electronics/computers/laptops",
            "children": []
          }
        ]
      }
    ]
  }
]
```

## Migration Considerations

### Existing Data
- Existing data entries without categories will have `category = null`
- No migration needed - backward compatible
- Users can update existing data to add categories

### Database Migration
H2 will automatically create the new tables and columns on application startup due to JPA auto-DDL.

For production databases, you may want to create explicit migration scripts:

```sql
-- Create categories table
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(500) NOT NULL UNIQUE,
    parent_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- Create index on path for fast lookups
CREATE INDEX idx_category_path ON categories(path);

-- Add category reference to data_storage
ALTER TABLE data_storage 
ADD COLUMN category_id BIGINT,
ADD CONSTRAINT fk_data_category FOREIGN KEY (category_id) REFERENCES categories(id);

-- Create index for joins
CREATE INDEX idx_data_category ON data_storage(category_id);
```

## Future Enhancements

1. **Category Statistics**: Count of data entries per category
2. **Category Search in Data**: Filter data by category path
3. **Category Renaming**: Ability to rename categories and update paths
4. **Category Deletion**: Delete empty categories
5. **Category Permissions**: Different access levels per category
6. **Category Metadata**: Add description, icon, color to categories
7. **Category Autocomplete**: Suggest existing categories in UI
8. **Bulk Category Operations**: Move multiple data entries between categories

## Security Considerations

- All category endpoints require authentication (JWT Bearer token)
- Categories are global (not user-specific) but data ownership is still enforced
- Consider adding category-level permissions in future versions

## Performance Notes

- Category creation uses database unique constraint for thread-safety
- Path-based lookups use database index
- Tree queries use JPA lazy loading to avoid N+1 problems
- Consider caching category tree for high-traffic scenarios

## Summary

The category structure provides:
✅ Hierarchical organization of data
✅ Automatic category creation
✅ REST API for category management
✅ UI integration with create/update forms
✅ Complete path storage for efficient queries
✅ Backward compatible with existing data
✅ Tree structure with parent-child relationships
