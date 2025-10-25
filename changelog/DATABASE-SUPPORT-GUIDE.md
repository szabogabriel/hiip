# Multi-Database Support Guide

## Current State
Your application currently uses **H2 in-memory database** with these configurations:
- Driver: `org.h2.Driver`
- Dialect: `org.hibernate.dialect.H2Dialect`
- URL: `jdbc:h2:mem:hiipdb`
- Scope: `runtime` (development/testing)

## What's Needed to Support Major Databases

### 1. **Add Runtime Database Drivers** ✅ Yes, Runtime Libraries Required

Each database requires its own JDBC driver JAR. Add these to `pom.xml`:

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MariaDB -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Oracle -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- SQL Server -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Keep H2 for development -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
    <scope>runtime</scope>
</dependency>
```

### 2. **Configuration Changes**

Your application already uses environment variables (excellent!), so it's mostly ready. Just need to document the configurations:

#### PostgreSQL Configuration
```properties
HIIP_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hiipdb
HIIP_DATASOURCE_DRIVER=org.postgresql.Driver
HIIP_DATASOURCE_USERNAME=hiip_user
HIIP_DATASOURCE_PASSWORD=secure_password
HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
```

#### MySQL Configuration
```properties
HIIP_DATASOURCE_URL=jdbc:mysql://localhost:3306/hiipdb?useSSL=false&serverTimezone=UTC
HIIP_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver
HIIP_DATASOURCE_USERNAME=hiip_user
HIIP_DATASOURCE_PASSWORD=secure_password
HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.MySQLDialect
```

#### Oracle Configuration
```properties
HIIP_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521:hiipdb
HIIP_DATASOURCE_DRIVER=oracle.jdbc.OracleDriver
HIIP_DATASOURCE_USERNAME=hiip_user
HIIP_DATASOURCE_PASSWORD=secure_password
HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.OracleDialect
```

#### SQL Server Configuration
```properties
HIIP_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=hiipdb;encrypt=true;trustServerCertificate=true
HIIP_DATASOURCE_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
HIIP_DATASOURCE_USERNAME=hiip_user
HIIP_DATASOURCE_PASSWORD=secure_password
HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.SQLServerDialect
```

#### MariaDB Configuration
```properties
HIIP_DATASOURCE_URL=jdbc:mariadb://localhost:3306/hiipdb
HIIP_DATASOURCE_DRIVER=org.mariadb.jdbc.Driver
HIIP_DATASOURCE_USERNAME=hiip_user
HIIP_DATASOURCE_PASSWORD=secure_password
HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.MariaDBDialect
```

### 3. **Code Compatibility Review**

Your code is already well-prepared! Here's why:

✅ **Good Practices Already in Place:**
- Using JPA/Hibernate (database-agnostic ORM)
- Standard JPQL queries (portable across databases)
- Using `@Entity` annotations (standard JPA)
- Environment variable configuration
- Proper data types (`String`, `Long`, `LocalDateTime`)

⚠️ **Potential Issues to Check:**

#### A. JSON Handling
Your `DataStorage.content` field stores JSON. Different databases handle this differently:

**Current (H2):**
```java
@Column(columnDefinition = "TEXT")
private String content;
```

**For Better Compatibility:**
```java
@Lob
@Column(columnDefinition = "TEXT")
private String content;
```

Or use database-specific JSON types:
```java
// PostgreSQL
@Column(columnDefinition = "jsonb")

// MySQL 5.7+
@Column(columnDefinition = "JSON")

// Oracle 21c+
@Column(columnDefinition = "JSON")
```

#### B. Element Collections (Tags)
Your tags are stored as `@ElementCollection`. This is compatible across databases but table names might vary.

#### C. Auto-increment IDs
```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

This works for most databases, but Oracle older versions prefer `SEQUENCE`:
```java
@GeneratedValue(strategy = GenerationType.AUTO)
```

### 4. **Database-Specific Considerations**

#### PostgreSQL
- **Best choice for production** - excellent JSON support, open-source, highly reliable
- Native `jsonb` type for efficient JSON queries
- Case-sensitive by default
- Excellent performance with large datasets

#### MySQL/MariaDB
- Very popular, good performance
- JSON support added in MySQL 5.7+
- Case-insensitive by default (configurable)
- MariaDB is fully open-source fork of MySQL

#### Oracle
- Enterprise-grade, extremely robust
- Requires commercial license (except XE edition)
- Excellent for large enterprises
- Native JSON support in 21c+
- Most complex setup

#### SQL Server
- Microsoft's enterprise database
- Good Windows integration
- Commercial license required (except Express)
- Good JSON support via FOR JSON

### 5. **Connection Pool Configuration (Recommended)**

Add HikariCP configuration (already included in Spring Boot):

```properties
# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=${HIIP_DB_POOL_SIZE:10}
spring.datasource.hikari.minimum-idle=${HIIP_DB_MIN_IDLE:5}
spring.datasource.hikari.connection-timeout=${HIIP_DB_CONNECTION_TIMEOUT:30000}
spring.datasource.hikari.idle-timeout=${HIIP_DB_IDLE_TIMEOUT:600000}
spring.datasource.hikari.max-lifetime=${HIIP_DB_MAX_LIFETIME:1800000}
```

### 6. **Migration Strategy**

#### Option A: Hibernate Auto-DDL (Current)
```properties
spring.jpa.hibernate.ddl-auto=update
```
✅ Simple, automatic
❌ Not recommended for production
❌ Can cause data loss

#### Option B: Flyway (Recommended for Production)
Add Flyway for database migrations:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- Database-specific Flyway support -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

#### Option C: Liquibase (Alternative)
```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

### 7. **Testing Strategy**

Use Testcontainers for testing with real databases:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
```

### 8. **Docker Compose for Local Development**

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: hiipdb
      POSTGRES_USER: hiip_user
      POSTGRES_PASSWORD: hiip_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  mysql:
    image: mysql:8
    environment:
      MYSQL_DATABASE: hiipdb
      MYSQL_USER: hiip_user
      MYSQL_PASSWORD: hiip_pass
      MYSQL_ROOT_PASSWORD: root_pass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  postgres_data:
  mysql_data:
```

## Summary: What You Need

### Minimal Changes (Just Runtime Libraries)
✅ **Add JDBC drivers to pom.xml** (5-10 lines)
✅ **Set environment variables** (already supported)
✅ **No code changes needed** (your code is database-agnostic)

### Recommended Changes for Production
1. Add multiple JDBC drivers
2. Consider Flyway/Liquibase for migrations
3. Add connection pool tuning
4. Add health checks per database
5. Create database-specific profiles

## Implementation Steps

### Step 1: Add All Drivers (Safe - No Breaking Changes)
```xml
<!-- Add to pom.xml dependencies section -->
```

### Step 2: Create Database Profiles
Create separate property files:
- `application-h2.properties` (default)
- `application-postgres.properties`
- `application-mysql.properties`
- `application-oracle.properties`

### Step 3: Document Environment Variables
Create `.env.example` with all supported databases

### Step 4: Test with Docker Compose
Verify each database works

## Your Application's Readiness

✅ **Already Database-Agnostic:**
- Using standard JPA/Hibernate
- Using JPQL (not native SQL)
- Using portable data types
- Configuration via environment variables

⚠️ **Minor Considerations:**
- JSON column type (currently TEXT, works everywhere)
- ID generation strategy (AUTO works for all)
- Case sensitivity (handle in queries if needed)

## Cost Considerations

| Database | License | Cost |
|----------|---------|------|
| H2 | Open Source | Free |
| PostgreSQL | Open Source | Free |
| MySQL | Open Source | Free |
| MariaDB | Open Source | Free |
| Oracle XE | Limited Free | Free (with limits) |
| Oracle SE | Commercial | $$$$ |
| SQL Server Express | Limited Free | Free (with limits) |
| SQL Server Standard | Commercial | $$$ |

## Recommendation

**For Your Application:**

1. **Development:** Keep H2 (already working)
2. **Production:** Use **PostgreSQL** - Best free option with excellent JSON support
3. **Enterprise:** Oracle or SQL Server if required by client

**Action Items:**
1. Add PostgreSQL and MySQL drivers to pom.xml (most common)
2. Create docker-compose.yml for local testing
3. Document environment variables
4. Test with PostgreSQL first
5. Add other databases as needed

The good news: Your code is already well-architected for multi-database support! You mainly just need to add the runtime JDBC drivers.
