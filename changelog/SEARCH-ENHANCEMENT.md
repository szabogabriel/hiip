# Search Enhancement: Combined Tags and Category Search

## Overview
Enhanced the data search functionality to support combined filtering by both tags and category. Both parameters are now **optional**, allowing for flexible search queries.

## Implementation Details

### API Changes

#### Updated Endpoint: `/api/v1/data/search`

**Before:**
```
GET /api/v1/data/search?tags=tag1,tag2
```
- Required `tags` parameter
- Could only search by tags

**After:**
```
GET /api/v1/data/search?tags=tag1,tag2&category=electronics/computers
GET /api/v1/data/search?tags=tag1
GET /api/v1/data/search?category=electronics/computers
GET /api/v1/data/search
```
- **Optional** `tags` parameter (comma-separated list)
- **Optional** `category` parameter (hierarchical path)
- Both parameters can be used together or independently
- If neither provided, returns all data (same as `/api/v1/data`)

### Search Behavior Matrix

| Tags Provided | Category Provided | Behavior |
|--------------|------------------|----------|
| ✅ Yes | ✅ Yes | Returns data matching **both** tags AND category |
| ✅ Yes | ❌ No | Returns data matching any of the tags |
| ❌ No | ✅ Yes | Returns data in the specified category |
| ❌ No | ❌ No | Returns all data for the user |

### Code Changes

#### 1. Repository Layer (`DataStorageRepository.java`)

Added new query methods:

```java
// Search by category only
List<DataStorage> findByCategoryAndOwnerAndHiddenFalse(Category category, String owner);

// Search by tags and category (combined)
@Query("SELECT DISTINCT d FROM DataStorage d JOIN d.tags t WHERE t IN :tags AND d.category = :category AND d.owner = :owner AND d.hidden = false")
List<DataStorage> findByTagsAndCategoryAndOwnerAndHiddenFalse(
    @Param("tags") List<String> tags, 
    @Param("category") Category category, 
    @Param("owner") String owner);
```

#### 2. Service Layer (`DataStorageService.java`)

Added flexible search method:

```java
public List<DataStorage> searchData(List<String> tags, Category category, String owner) {
    // Both tags and category provided
    if (tags != null && !tags.isEmpty() && category != null) {
        return dataStorageRepository.findByTagsAndCategoryAndOwnerAndHiddenFalse(tags, category, owner);
    }
    // Only tags provided
    else if (tags != null && !tags.isEmpty()) {
        return dataStorageRepository.findByTagsInAndOwnerAndHiddenFalse(tags, owner);
    }
    // Only category provided
    else if (category != null) {
        return dataStorageRepository.findByCategoryAndOwnerAndHiddenFalse(category, owner);
    }
    // Neither provided - return all data
    else {
        return dataStorageRepository.findByOwnerAndHiddenFalse(owner);
    }
}
```

#### 3. Facade Layer (`DataStorageFacadeService.java`)

Added search facade with category path resolution:

```java
public List<DataStorageResponse> searchData(List<String> tags, String categoryPath, String owner) {
    // Resolve category if provided
    Category category = null;
    if (categoryPath != null && !categoryPath.trim().isEmpty()) {
        category = categoryService.findByPath(categoryPath);
        if (category == null) {
            return List.of(); // Category doesn't exist, no results
        }
    }
    
    return dataStorageService.searchData(tags, category, owner)
            .stream()
            .map(DataStorageResponse::new)
            .collect(Collectors.toList());
}
```

#### 4. Controller Layer (`DataStorageController.java`)

Updated endpoint to accept optional parameters:

```java
@GetMapping("/search")
public ResponseEntity<List<DataStorageResponse>> searchData(
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) String category,
        Authentication authentication) {
    
    String owner = authentication.getName();
    List<DataStorageResponse> data = dataStorageFacadeService.searchData(tags, category, owner);
    
    return ResponseEntity.ok(data);
}
```

#### 5. Category Service Enhancement (`CategoryService.java`)

Added method to find category without creating:

```java
public Category findByPath(String categoryPath) {
    if (categoryPath == null || categoryPath.trim().isEmpty()) {
        return null;
    }
    
    String normalizedPath = normalizePath(categoryPath);
    if (normalizedPath.isEmpty()) {
        return null;
    }
    
    return categoryRepository.findByPath(normalizedPath).orElse(null);
}
```

## Usage Examples

### Example 1: Search by Tags Only
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?tags=urgent,important" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Returns all data with tags "urgent" OR "important"

### Example 2: Search by Category Only
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=work/projects/2024" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Returns all data in the "work/projects/2024" category

### Example 3: Combined Search (Tags AND Category)
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?tags=important&category=work/projects/2024" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Returns data that:
- Has tag "important" **AND**
- Is in category "work/projects/2024"

### Example 4: Get All Data (No Filters)
```bash
curl -X GET "http://localhost:8080/api/v1/data/search" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Returns all data for the authenticated user

### Example 5: Non-existent Category
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=doesnt/exist" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Returns empty array `[]` (category doesn't exist, so no results)

## Swagger/OpenAPI Documentation

The endpoint is documented in Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

**Updated Description:**
> Search data storage entries by tags and/or category. Both parameters are optional.

**Parameters:**
- `tags` (query, optional): Array of strings - Tags to search for
- `category` (query, optional): String - Category path to filter by

## Benefits

1. **Flexibility**: Users can filter by tags, category, or both
2. **Optional Parameters**: No parameters required - works as "get all" endpoint
3. **Backward Compatible**: Existing tag-only searches still work
4. **Precise Filtering**: Combine criteria for more specific results
5. **Intuitive**: Empty category returns empty results (doesn't create it)

## Technical Notes

### Category Path Normalization
- Category paths are normalized before search (trim, remove duplicate slashes)
- Case-sensitive matching
- Non-existent categories return empty results (no auto-creation on search)

### Tag Matching
- Tags use OR logic: any tag match returns the data
- When combined with category: tags OR + category AND

### Performance Considerations
- All queries filter by `owner` and `hidden = false`
- Uses indexed columns for efficient searches
- JPQL queries optimized with proper JOIN syntax
- Distinct results to avoid duplicates from tag joins

## Migration Notes

### Breaking Changes
None - this is a backward-compatible enhancement.

### Database Changes
No new database schema changes required. Uses existing:
- `data_storage` table with `category_id` foreign key
- `data_storage_tags` junction table
- Existing indexes on `owner`, `hidden`, `category_id`

## Testing Scenarios

- [x] Search with tags only
- [x] Search with category only  
- [x] Search with both tags and category
- [x] Search with no parameters (returns all)
- [x] Search with non-existent category (returns empty)
- [x] Search with empty tags array (returns all or by category)
- [x] Verify only user's own data is returned
- [x] Verify hidden data is excluded
- [x] Test category path normalization
- [x] Test hierarchical category paths

## Related Files

- `/src/main/java/com/hiip/datastorage/controller/DataStorageController.java`
- `/src/main/java/com/hiip/datastorage/service/DataStorageFacadeService.java`
- `/src/main/java/com/hiip/datastorage/service/DataStorageService.java`
- `/src/main/java/com/hiip/datastorage/service/CategoryService.java`
- `/src/main/java/com/hiip/datastorage/repository/DataStorageRepository.java`

## Future Enhancements

Potential improvements:
- Add support for category hierarchy search (include subcategories)
- Add support for tag AND logic (all tags must match)
- Add support for regex or wildcard category matching
- Add pagination for large result sets
- Add sorting options
