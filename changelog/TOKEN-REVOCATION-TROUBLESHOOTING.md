# Token Revocation Troubleshooting Guide

## Issue
After implementing token revocation, the logout doesn't seem to invalidate the access token. Users can still make requests with the same token after logging out.

## What Was Implemented

### 1. New Components Added:
- `RevokedToken` entity - Database table for blacklisted tokens
- `RevokedTokenRepository` - JPA repository for token blacklist
- `TokenRevocationService` - Service to manage revocation with logging
- Updated `JwtAuthenticationFilter` - Checks blacklist before authentication
- Updated `AuthFacadeService` - Logout method now revokes tokens
- Updated `AuthController` - Logout endpoint accepts HttpServletRequest
- Updated `DataStorageApplication` - Added @EnableScheduling

### 2. How It Should Work:
```
Login → Get Token → Use Token (✅) → Logout → Token Added to Blacklist → Use Token (❌ 401)
```

## Testing Steps

### Option 1: Use the Test Script
```bash
# Make sure your application is running first
cd /home/gszabo/Projects/Else/hiit
./scripts/test-token-revocation.sh
```

The script will:
1. Login as admin (hiipa/hiipa)
2. Use the token to access /api/v1/data
3. Logout (revoke token)
4. Try to use the same token again
5. Report if revocation is working

### Option 2: Manual Testing
```bash
# 1. Start the application
mvn spring-boot:run

# In another terminal:

# 2. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hiipa","password":"hiipa"}' \
  | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "Token: $TOKEN"

# 3. Test access (should work)
curl -X GET http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer $TOKEN"

# 4. Logout
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"

# 5. Test access again (should fail with 401)
curl -X GET http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting

### Problem 1: Token still works after logout

**Check 1: Application Restart**
```bash
# The application MUST be restarted with the new code
# Stop the running application and start it again
mvn spring-boot:run
```

**Check 2: Database Table Created**
```bash
# Connect to H2 console (if enabled) or check logs
# Look for: "CREATE TABLE revoked_tokens"
grep "revoked_tokens" logs/application.log
```

**Check 3: Check Application Logs**
```bash
# Look for these log messages:
# - "Token revoked for user: hiipa"
# - "Token revocation check"
# - "Logout attempt"

# Enable debug logging if needed:
# Add to application.properties:
# logging.level.com.hiip.datastorage.service.authentication.TokenRevocationService=DEBUG
# logging.level.com.hiip.datastorage.service.controller.AuthFacadeService=DEBUG
```

**Check 4: Verify Token is Being Saved**
```sql
-- If H2 console is enabled (hiip.h2.console.enabled=true)
-- Go to: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:mem:hiipdb
-- Username: sa
-- Password: (empty)

SELECT * FROM revoked_tokens;
-- Should show entries after logout
```

### Problem 2: Getting 401 immediately after login

**Possible causes:**
- JWT secret not configured properly
- System clock issues
- Token expiration too short

**Solution:**
```properties
# Check application.properties:
hiip.jwt.secret=c+rN1caROfq4wy6BOgC+767E857k8jA4ki5a19BGjf3cxR+XWLRDX7D5CFDHcjbRiPD5w+F5Dok6cZqS6IFGjg==
hiip.jwt.expiration=86400000  # 24 hours
```

### Problem 3: Database errors on startup

**If you see:**
```
Table "REVOKED_TOKENS" not found
```

**Solution:**
```properties
# Ensure in application.properties:
spring.jpa.hibernate.ddl-auto=update

# Or manually create table:
CREATE TABLE revoked_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    revoked_at TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NOT NULL
);
CREATE INDEX idx_token ON revoked_tokens(token);
CREATE INDEX idx_expiration ON revoked_tokens(expiration_date);
```

### Problem 4: Performance Issues

**If blacklist table grows too large:**

**Solution 1: Check cleanup task**
```bash
# Cleanup runs daily at 3 AM
# Check logs for: "Cleaned up X expired revoked tokens"

# Manually trigger cleanup:
# (Add a controller endpoint or use scheduled task)
```

**Solution 2: Adjust token expiration**
```properties
# Shorter tokens = smaller blacklist
hiip.jwt.expiration=3600000  # 1 hour instead of 24
```

**Solution 3: Use Redis (future enhancement)**
- Replace database with Redis for faster lookups
- Tokens auto-expire using Redis TTL

## Verification Checklist

✅ Application rebuilt with `mvn clean package`  
✅ Application restarted  
✅ `revoked_tokens` table created in database  
✅ Logout returns success message  
✅ Token appears in `revoked_tokens` table  
✅ Subsequent requests with revoked token return 401  
✅ New login creates new valid token  
✅ Logs show "Token revoked for user: ..."  

## Expected Log Output

When logout is working correctly, you should see:

```
INFO  c.h.d.s.c.AuthFacadeService - Logout attempt - Authorization header present: true
INFO  c.h.d.s.c.AuthFacadeService - Logout - Extracted username: hiipa
INFO  c.h.d.s.a.TokenRevocationService - Token revoked for user: hiipa (expires: 2025-10-26T11:22:00)
INFO  c.h.d.s.c.AuthFacadeService - Token revoked successfully for user: hiipa
```

When revoked token is used:

```
DEBUG c.h.d.s.a.TokenRevocationService - Token revocation check: eyJhbGciOiJIUzI1NiI... - REVOKED
WARN  c.h.d.s.JwtAuthenticationFilter - JWT Token has been revoked
```

## Admin Credentials

**Default:**
- Username: `hiipa`
- Password: `hiipa`

**Can be changed via environment variables:**
```bash
export HIIP_ADMIN_USERNAME=admin
export HIIP_ADMIN_PASSWORD=admin123
```

Or in application.properties:
```properties
hiip.admin.username=admin
hiip.admin.password=admin123
```

## Next Steps If Still Not Working

1. **Check if application is running the new code:**
   ```bash
   # Verify JAR was rebuilt
   ls -lh target/data-storage-1.0.0.jar
   # Should show recent timestamp
   ```

2. **Enable H2 Console to inspect database:**
   ```properties
   # Add to application.properties or set environment variable:
   spring.h2.console.enabled=true
   # Or:
   export HIIP_H2_CONSOLE_ENABLED=true
   ```
   Then visit: http://localhost:8080/h2-console

3. **Check for errors in logs:**
   ```bash
   # Look for exceptions related to:
   # - RevokedToken
   # - TokenRevocationService
   # - JwtAuthenticationFilter
   ```

4. **Verify JwtAuthenticationFilter is being used:**
   ```bash
   # Should see in startup logs:
   grep "JwtAuthenticationFilter" logs/application.log
   ```

5. **Test with Swagger UI:**
   - Go to: http://localhost:8080/swagger-ui/index.html
   - Login and get token
   - Use "Authorize" button to add token
   - Try to access endpoints
   - Logout
   - Try to access endpoints again (should fail)

## Contact Information

If the issue persists after following all troubleshooting steps:

1. Share application logs showing:
   - Startup messages
   - Logout attempt
   - Subsequent access attempt

2. Share output of test script:
   ```bash
   ./scripts/test-token-revocation.sh > test-output.txt 2>&1
   ```

3. Share database contents:
   ```sql
   SELECT * FROM revoked_tokens;
   ```
