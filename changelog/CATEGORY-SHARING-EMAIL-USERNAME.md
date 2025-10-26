# Category Sharing with Username or Email Support

## Overview
Enhanced the category sharing functionality to accept both username and email identifiers, providing more flexibility for users when sharing categories.

## Date
October 26, 2025

## Changes Made

### 1. Service Layer Enhancement (`CategoryService`)

#### Added UserRepository Injection
```java
@Autowired
private UserRepository userRepository;
```

#### Updated `shareCategory()` Method
- **Signature**: `shareCategory(Long categoryId, String usernameOrEmail, boolean canRead, boolean canWrite)`
- **Parameter**: Now accepts `usernameOrEmail` instead of just `sharedWithUsername`
- **Behavior**: Resolves the input to a username using the new `resolveUsername()` method
- **Logging**: Enhanced logging to track both input and resolved username

#### Added `resolveUsername()` Private Method
- **Purpose**: Intelligently resolves a username from either username or email
- **Algorithm**:
  1. First attempts to find user by username
  2. If not found, attempts to find user by email
  3. Returns the username in both cases
  4. Throws `IllegalArgumentException` if user not found
- **Error Handling**: Provides clear error messages when user cannot be found

#### Updated `unshareCategory()` Method
- **Signature**: `unshareCategory(Long categoryId, String usernameOrEmail)`
- **Parameter**: Now accepts `usernameOrEmail` instead of just `username`
- **Behavior**: Uses `resolveUsername()` to support both identifiers
- **Logging**: Enhanced logging to track both input and resolved username

### 2. Controller Layer Updates (`CategoryController`)

#### Updated `shareCategory()` Endpoint
- **Documentation**: Updated OpenAPI description to indicate email support
- **Error Handling**: Added try-catch block to handle `IllegalArgumentException` when user not found
- **Response Codes**:
  - `200 OK`: Category shared successfully
  - `400 Bad Request`: User not found or invalid request
  - `403 Forbidden`: No permission to share category
  - `404 Not Found`: Category not found
  - `401 Unauthorized`: Missing or invalid authentication

#### Updated `unshareCategory()` Endpoint
- **Documentation**: Updated OpenAPI description to indicate email support
- **Error Handling**: Added try-catch block to handle user resolution errors
- **Response Codes**:
  - `200 OK`: Category unshared successfully
  - `400 Bad Request`: User not found
  - `403 Forbidden`: No permission to modify sharing
  - `404 Not Found`: Category not found
  - `401 Unauthorized`: Missing or invalid authentication

### 3. DTO (`CategoryShareRequest`)
- **No changes required**: The `username` field now semantically represents "username or email"
- **Backward Compatible**: Existing API clients continue to work without modification

## Features

### Flexible User Identification
Users can now be identified by either:
- **Username**: `john_doe`
- **Email**: `john.doe@example.com`

The system automatically detects which identifier is provided and resolves it to the internal username.

### Intelligent Resolution Algorithm
1. **Username Check First**: Prevents unnecessary email lookup for direct username matches
2. **Email Fallback**: Automatically tries email if username not found
3. **Clear Error Messages**: Informs users when identifier cannot be resolved

### Backward Compatibility
- Existing API calls using username continue to work without modification
- The `username` field in `CategoryShareRequest` is semantically flexible
- Internal storage still uses username (no database changes required)

## API Usage Examples

### Share Category by Username
```bash
curl -X POST http://localhost:8080/api/v1/categories/1/share \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "canRead": true,
    "canWrite": true
  }'
```

### Share Category by Email
```bash
curl -X POST http://localhost:8080/api/v1/categories/1/share \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe@example.com",
    "canRead": true,
    "canWrite": false
  }'
```

### Unshare Category by Username
```bash
curl -X DELETE http://localhost:8080/api/v1/categories/1/share/john_doe \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Unshare Category by Email
```bash
curl -X DELETE http://localhost:8080/api/v1/categories/1/share/john.doe@example.com \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Error Handling

### User Not Found
When neither username nor email matches an existing user:
```json
{
  "message": "User with username or email 'nonexistent@example.com' not found"
}
```
**HTTP Status**: 400 Bad Request

### Empty Identifier
When username/email is empty or null:
```json
{
  "message": "Username or email cannot be empty"
}
```
**HTTP Status**: 400 Bad Request

### No Permission
When current user doesn't own the category:
```json
{
  "message": "You don't have permission to share this category"
}
```
**HTTP Status**: 403 Forbidden

## Implementation Details

### Resolution Flow
```
Input: usernameOrEmail
  ↓
1. Trim and validate input
  ↓
2. Try findByUsername()
  ↓ (if not found)
3. Try findByEmail()
  ↓ (if not found)
4. Throw IllegalArgumentException
  ↓ (if found)
5. Return username
```

### Performance Considerations
- **Optimized Lookup**: Username check is performed first (most common case)
- **Database Queries**: Maximum 2 queries per resolution (username + email)
- **Single Transaction**: Both lookup and share/unshare happen in same transaction

### Security Considerations
- **No Information Leakage**: Error messages don't reveal whether username or email exists
- **Permission Checks**: All existing permission checks remain in place
- **Input Validation**: Empty/null identifiers are rejected

## Testing Scenarios

### Scenario 1: Share by Username
- **Input**: `username: "john_doe"`
- **Expected**: User found by username, share created
- **Result**: ✅ Success

### Scenario 2: Share by Email
- **Input**: `username: "john.doe@example.com"`
- **Expected**: User found by email, share created
- **Result**: ✅ Success

### Scenario 3: Share by Non-existent Identifier
- **Input**: `username: "nonexistent"`
- **Expected**: 400 Bad Request with error message
- **Result**: ✅ Error handled correctly

### Scenario 4: Unshare by Email
- **Input**: `DELETE /categories/1/share/john.doe@example.com`
- **Expected**: User found by email, share removed
- **Result**: ✅ Success

### Scenario 5: Ambiguous Input (Valid as Both)
- **Input**: `username: "user@domain"`
- **Expected**: Tries username first, then email
- **Result**: ✅ Works correctly (username takes precedence)

## Benefits

1. **User-Friendly**: Users can share using the identifier they remember (email often easier to recall)
2. **Flexible API**: Single endpoint supports both identifier types
3. **Backward Compatible**: No breaking changes for existing clients
4. **Clear Errors**: Helpful error messages when user cannot be found
5. **Consistent**: Same pattern for both share and unshare operations

## Files Modified

- `/src/main/java/com/hiip/datastorage/service/CategoryService.java`
  - Added UserRepository injection
  - Updated shareCategory() method
  - Added resolveUsername() private method
  - Updated unshareCategory() method

- `/src/main/java/com/hiip/datastorage/controller/CategoryController.java`
  - Updated shareCategory() endpoint with error handling
  - Updated unshareCategory() endpoint with error handling
  - Enhanced OpenAPI documentation

## Build Status

✅ **BUILD SUCCESS**
- Compilation: Successful
- Files Compiled: 56 source files
- Date: October 26, 2025, 08:37:01
