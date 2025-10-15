# Wildcard Search Implementation: Using '*' as SQL LIKE Wildcard

## Overview
Implemented wildcard pattern matching for category searches using `*` as a wildcard character, equivalent to SQL's `%` operator. This allows users to search for categories using patterns like `work/*`, `*project*`, or `*/2024`.

## Feature Description
When searching by category, users can now use `*` as a wildcard to match any sequence of characters:
- `work/*` - Matches any category starting with "work/" (e.g., work/projects, work/notes)
- `*project*` - Matches any category containing "project" (e.g., work/project, personal/projects)
- `*/2024` - Matches any category ending with "/2024"
- `*` - Matches all categories (any path)

## Implementation

### 1. Pattern Detection in DataStorageFacadeService
Added logic to detect wildcards and route to pattern matching:

```java
public List<DataStorageResponse> searchData(List<String> tags, String categoryPath, String owner) {
    // Check if category pattern contains wildcards
    boolean hasWildcard = categoryPath != null && categoryPath.contains("*");
    
    if (hasWildcard) {
        // Use pattern matching search for wildcards
        return dataStorageService.searchDataByPattern(tags, categoryPath, owner)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
    } else {
        // Use exact match for normal searches
        // ...
    }
}
```

### 2. Added Pattern Search in DataStorageService
Created new method to handle wildcard patterns:

```java
public List<DataStorage> searchDataByPattern(List<String> tags, String categoryPattern, String owner) {
    // Convert '*' to SQL '%' wildcard
    String sqlPattern = categoryPattern.replace("*", "%");
    
    // Both tags and category pattern provided
    if (tags != null && !tags.isEmpty()) {
        return dataStorageRepository.findByTagsAndCategoryPathLikeAndOwnerAndHiddenFalse(tags, sqlPattern, owner);
    }
    // Only category pattern provided
    else {
        return dataStorageRepository.findByCategoryPathLikeAndOwnerAndHiddenFalse(sqlPattern, owner);
    }
}
```

### 3. Added Repository Methods for Pattern Matching
Added two new JPQL queries in DataStorageRepository:

```java
// Search by category path pattern (with wildcards)
@Query("SELECT d FROM DataStorage d WHERE d.category.path LIKE :pathPattern AND d.owner = :owner AND d.hidden = false")
List<DataStorage> findByCategoryPathLikeAndOwnerAndHiddenFalse(
    @Param("pathPattern") String pathPattern, 
    @Param("owner") String owner);

// Search by tags and category path pattern (with wildcards)
@Query("SELECT DISTINCT d FROM DataStorage d JOIN d.tags t WHERE t IN :tags AND d.category.path LIKE :pathPattern AND d.owner = :owner AND d.hidden = false")
List<DataStorage> findByTagsAndCategoryPathLikeAndOwnerAndHiddenFalse(
    @Param("tags") List<String> tags,
    @Param("pathPattern") String pathPattern, 
    @Param("owner") String owner);
```

### 4. Fixed CategoryRepository
Ensured both exact match and pattern match methods exist:

```java
/**
 * Find a category by its exact path
 */
Optional<Category> findByPath(String path);

/**
 * Find a category by its path using LIKE (for wildcard searches)
 */
@Query("SELECT c FROM Category c WHERE c.path LIKE :path")
Optional<Category> findByPathLike(String path);
```

### 5. Updated API Documentation
Updated controller Swagger documentation:

```java
@Operation(
    summary = "Search data by tags and/or category",
    description = "Search data storage entries by tags and/or category. Both parameters are optional. " +
                 "Category supports wildcard patterns using '*' (equivalent to SQL '%'). " +
                 "Examples: 'work/*' matches 'work/projects', 'work/notes'; '*project*' matches any path containing 'project'."
)
```

## Usage Examples

### Example 1: Find All Categories Starting with "work/"
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=work/*" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Matches:**
- work/projects
- work/notes
- work/archive/2024

**Result:** Returns all data in categories starting with "work/"

### Example 2: Find All Categories Containing "project"
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=*project*" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Matches:**
- work/projects
- personal/project-ideas
- archive/old-projects

**Result:** Returns all data in categories containing "project"

### Example 3: Find All Categories Ending with "/2024"
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=*/2024" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Matches:**
- work/2024
- personal/archive/2024
- projects/active/2024

**Result:** Returns all data in categories ending with "/2024"

### Example 4: Wildcard with Tags
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?tags=urgent&category=work/*" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Result:** Returns urgent items in any category starting with "work/"

### Example 5: All Categories
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=*" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Result:** Returns all data from all categories (equivalent to no category filter)

### Example 6: Exact Match (No Wildcards)
```bash
curl -X GET "http://localhost:8080/api/v1/data/search?category=work/projects" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
**Result:** Returns data only from the exact "work/projects" category

## Wildcard Pattern Behavior

| Pattern | Example | Matches |
|---------|---------|---------|
| `prefix/*` | `work/*` | work/projects, work/notes, work/archive |
| `*suffix` | `*/2024` | work/2024, personal/2024, archive/2024 |
| `*middle*` | `*project*` | work/projects, my-project, project-ideas |
| `*` | `*` | All categories |
| `exact` | `work/projects` | Only work/projects (exact match) |
| `pre*/suf*` | `work/*/2024` | work/projects/2024, work/archive/2024 |

### Technical Details

**Wildcard Conversion:**
- User input: `work/*` 
- Converted to SQL: `work/%`
- SQL LIKE operator matches patterns

**Case Sensitivity:**
- Pattern matching is case-sensitive (database dependent)
- `Work/*` ≠ `work/*`

**Multiple Wildcards:**
- Supported: `work/*/2024` → `work/%/2024`
- Matches: work/projects/2024, work/archive/2024

**Escape Characters:**
- If you need literal `*` in path, currently not supported
- Future enhancement could add escape sequences

## Benefits

1. **Flexible Searching** - Find data across category hierarchies
2. **Pattern Matching** - Use familiar `*` wildcard syntax
3. **SQL LIKE Power** - Leverages SQL's pattern matching capabilities
4. **Performance** - Database-level pattern matching is efficient
5. **Intuitive** - `*` wildcard familiar to most users
6. **Hierarchical Queries** - Easy to search category trees (e.g., `work/*`)

## Testing Scenarios

- [x] Pattern `work/*` matches work/projects, work/notes
- [x] Pattern `*project*` matches any path with "project"
- [x] Pattern `*/2024` matches paths ending with /2024
- [x] Pattern `*` matches all categories
- [x] Exact match `work/projects` works without wildcards
- [x] Wildcard with tags combines correctly
- [x] Empty category returns all data
- [x] Non-existent exact category returns empty
- [x] Multiple wildcards in pattern work
- [x] No 500 errors with wildcard patterns

## Performance Considerations

### Indexed Searches
- Prefix patterns (`work/*`) can use indexes efficiently
- Suffix patterns (`*/2024`) may require full table scan
- Infix patterns (`*project*`) require full table scan

### Best Practices
- Use prefix patterns when possible for better performance
- Combine with tags to reduce result set
- Consider category naming conventions for efficient wildcard queries

## Implementation Details

### Wildcard Detection
```java
boolean hasWildcard = categoryPath != null && categoryPath.contains("*");
```

### Wildcard Conversion
```java
String sqlPattern = categoryPattern.replace("*", "%");
```

### JPQL Query
```sql
SELECT d FROM DataStorage d 
WHERE d.category.path LIKE :pathPattern 
  AND d.owner = :owner 
  AND d.hidden = false
```

## Related Changes

### Files Modified
1. `/src/main/java/com/hiip/datastorage/service/DataStorageFacadeService.java`
   - Added wildcard detection logic
   - Routes to pattern search when `*` detected

2. `/src/main/java/com/hiip/datastorage/service/DataStorageService.java`
   - Added `searchDataByPattern()` method
   - Converts `*` to SQL `%` wildcard

3. `/src/main/java/com/hiip/datastorage/repository/DataStorageRepository.java`
   - Added `findByCategoryPathLikeAndOwnerAndHiddenFalse()` 
   - Added `findByTagsAndCategoryPathLikeAndOwnerAndHiddenFalse()`
   - Both use JPQL LIKE operator

4. `/src/main/java/com/hiip/datastorage/repository/CategoryRepository.java`
   - Added `findByPath()` for exact matching
   - Kept `findByPathLike()` for pattern matching

5. `/src/main/java/com/hiip/datastorage/controller/DataStorageController.java`
   - Updated Swagger documentation with wildcard examples

## Migration Notes

### Breaking Changes
None - this is a backward-compatible enhancement.

### Behavior Changes
- Category search now supports wildcard patterns with `*`
- Exact matches work as before when no `*` present
- Pattern matching uses SQL LIKE operator

## Future Enhancements

Additional wildcard features that could be added:
- `?` for single character wildcard (like SQL `_`)
- Escape sequences for literal `*` in paths
- Regular expression support
- Case-insensitive matching option
- Wildcard validation and sanitization

## Examples Summary

| Use Case | Pattern | Description |
|----------|---------|-------------|
| All work items | `work/*` | Everything under work |
| 2024 items | `*/2024` | All year 2024 categories |
| Project-related | `*project*` | Anything with "project" |
| Everything | `*` | All categories |
| Specific path | `work/projects` | Exact match only |
| Deep wildcard | `work/*/active` | work/.../active |

## Notes

- The `*` wildcard is purely semantic (application-level)
- It's not passed to SQL queries
- Safe from SQL injection
- Can be extended with more wildcard patterns in the future
- Logging shows when wildcard is detected for debugging
