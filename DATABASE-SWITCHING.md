# Database Switching Guide

## ✅ All Database Drivers Are Now Included!

Your application now includes JDBC drivers for all major databases:
- ✅ H2 (in-memory, for development)
- ✅ PostgreSQL
- ✅ MySQL
- ✅ MariaDB
- ✅ Microsoft SQL Server
- ⚪ Oracle (commented out, uncomment if needed)

**Switching databases is now just a matter of configuration!**

---

## Quick Switch: Set Environment Variables

### Option 1: Command Line (Linux/Mac)

#### H2 (Default - In-Memory)
```bash
# No configuration needed - this is the default
# Or explicitly:
export HIIP_DATASOURCE_URL="jdbc:h2:mem:hiipdb"
export HIIP_DATASOURCE_DRIVER="org.h2.Driver"
export HIIP_DATASOURCE_USERNAME="sa"
export HIIP_DATASOURCE_PASSWORD=""
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.H2Dialect"
```

#### PostgreSQL
```bash
export HIIP_DATASOURCE_URL="jdbc:postgresql://localhost:5432/hiipdb"
export HIIP_DATASOURCE_DRIVER="org.postgresql.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
```

#### MySQL
```bash
export HIIP_DATASOURCE_URL="jdbc:mysql://localhost:3306/hiipdb?useSSL=false&serverTimezone=UTC"
export HIIP_DATASOURCE_DRIVER="com.mysql.cj.jdbc.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"
```

#### MariaDB
```bash
export HIIP_DATASOURCE_URL="jdbc:mariadb://localhost:3306/hiipdb"
export HIIP_DATASOURCE_DRIVER="org.mariadb.jdbc.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.MariaDBDialect"
```

#### SQL Server
```bash
export HIIP_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=hiipdb;encrypt=true;trustServerCertificate=true"
export HIIP_DATASOURCE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver"
export HIIP_DATASOURCE_USERNAME="sa"
export HIIP_DATASOURCE_PASSWORD="HiipPass123!"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.SQLServerDialect"
```

Then run:
```bash
mvn spring-boot:run
# or
java -jar target/data-storage-1.0.0.jar
```

---

### Option 2: Using .env File

1. **Copy the example:**
```bash
cp .env.example .env
```

2. **Edit `.env`** and uncomment the database section you want to use

3. **Load environment variables:**
```bash
set -a
source .env
set +a
```

4. **Run the application:**
```bash
mvn spring-boot:run
```

---

### Option 3: Docker Compose with Environment

Create a `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      # Switch database by changing these values
      HIIP_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hiipdb
      HIIP_DATASOURCE_DRIVER: org.postgresql.Driver
      HIIP_DATASOURCE_USERNAME: hiip_user
      HIIP_DATASOURCE_PASSWORD: hiip_pass
      HIIP_HIBERNATE_DIALECT: org.hibernate.dialect.PostgreSQLDialect
    depends_on:
      - postgres
```

---

### Option 4: Maven Profiles (Advanced)

Create database-specific profiles in `pom.xml`:

```xml
<profiles>
    <profile>
        <id>h2</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <db.url>jdbc:h2:mem:hiipdb</db.url>
            <db.driver>org.h2.Driver</db.driver>
            <db.dialect>org.hibernate.dialect.H2Dialect</db.dialect>
        </properties>
    </profile>
    
    <profile>
        <id>postgres</id>
        <properties>
            <db.url>jdbc:postgresql://localhost:5432/hiipdb</db.url>
            <db.driver>org.postgresql.Driver</db.driver>
            <db.dialect>org.hibernate.dialect.PostgreSQLDialect</db.dialect>
        </properties>
    </profile>
    
    <profile>
        <id>mysql</id>
        <properties>
            <db.url>jdbc:mysql://localhost:3306/hiipdb</db.url>
            <db.driver>com.mysql.cj.jdbc.Driver</db.driver>
            <db.dialect>org.hibernate.dialect.MySQLDialect</db.dialect>
        </properties>
    </profile>
</profiles>
```

Then run:
```bash
mvn spring-boot:run -Ppostgres
mvn spring-boot:run -Pmysql
```

---

## Testing Database Switch

### 1. Start with H2 (No Setup Required)
```bash
# Just run - uses H2 by default
mvn spring-boot:run
```

### 2. Switch to PostgreSQL
```bash
# Start PostgreSQL via Docker
docker-compose up -d postgres

# Set environment variables
export HIIP_DATASOURCE_URL="jdbc:postgresql://localhost:5432/hiipdb"
export HIIP_DATASOURCE_DRIVER="org.postgresql.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"

# Restart application
mvn spring-boot:run
```

### 3. Switch to MySQL
```bash
# Stop current app (Ctrl+C)
# Start MySQL via Docker
docker-compose up -d mysql

# Change environment variables
export HIIP_DATASOURCE_URL="jdbc:mysql://localhost:3306/hiipdb?useSSL=false&serverTimezone=UTC"
export HIIP_DATASOURCE_DRIVER="com.mysql.cj.jdbc.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"

# Restart application
mvn spring-boot:run
```

---

## Verify Database Connection

Check application logs for successful connection:

**H2:**
```
HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:hiipdb
```

**PostgreSQL:**
```
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
```

**MySQL:**
```
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
```

**SQL Server:**
```
HikariPool-1 - Added connection com.microsoft.sqlserver.jdbc.SQLServerConnection@...
```

---

## Production Deployment Examples

### Kubernetes ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hiip-config
data:
  HIIP_DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/hiipdb"
  HIIP_DATASOURCE_DRIVER: "org.postgresql.Driver"
  HIIP_DATASOURCE_USERNAME: "hiip_user"
  HIIP_HIBERNATE_DIALECT: "org.hibernate.dialect.PostgreSQLDialect"
```

### Docker Run
```bash
docker run -d \
  -e HIIP_DATASOURCE_URL="jdbc:postgresql://db-host:5432/hiipdb" \
  -e HIIP_DATASOURCE_DRIVER="org.postgresql.Driver" \
  -e HIIP_DATASOURCE_USERNAME="hiip_user" \
  -e HIIP_DATASOURCE_PASSWORD="hiip_pass" \
  -e HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect" \
  -p 8080:8080 \
  hiip:latest
```

### Systemd Service
```ini
[Service]
Environment="HIIP_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hiipdb"
Environment="HIIP_DATASOURCE_DRIVER=org.postgresql.Driver"
Environment="HIIP_DATASOURCE_USERNAME=hiip_user"
Environment="HIIP_DATASOURCE_PASSWORD=hiip_pass"
Environment="HIIP_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect"
ExecStart=/usr/bin/java -jar /opt/hiip/data-storage-1.0.0.jar
```

---

## Database-Specific Connection Strings

### PostgreSQL Variants
```bash
# Standard
jdbc:postgresql://localhost:5432/hiipdb

# With SSL
jdbc:postgresql://localhost:5432/hiipdb?ssl=true&sslmode=require

# With connection parameters
jdbc:postgresql://localhost:5432/hiipdb?currentSchema=public&ssl=true

# Cloud (AWS RDS)
jdbc:postgresql://myinstance.123456789012.us-east-1.rds.amazonaws.com:5432/hiipdb

# Cloud (Azure)
jdbc:postgresql://myserver.postgres.database.azure.com:5432/hiipdb?sslmode=require
```

### MySQL Variants
```bash
# Standard
jdbc:mysql://localhost:3306/hiipdb

# With timezone and SSL
jdbc:mysql://localhost:3306/hiipdb?useSSL=true&serverTimezone=UTC

# Cloud (AWS RDS)
jdbc:mysql://myinstance.123456789012.us-east-1.rds.amazonaws.com:3306/hiipdb

# Cloud (Azure)
jdbc:mysql://myserver.mysql.database.azure.com:3306/hiipdb?useSSL=true
```

### SQL Server Variants
```bash
# Standard
jdbc:sqlserver://localhost:1433;databaseName=hiipdb

# With encryption
jdbc:sqlserver://localhost:1433;databaseName=hiipdb;encrypt=true;trustServerCertificate=true

# Windows Authentication
jdbc:sqlserver://localhost:1433;databaseName=hiipdb;integratedSecurity=true

# Cloud (Azure SQL)
jdbc:sqlserver://myserver.database.windows.net:1433;database=hiipdb;encrypt=true
```

---

## Common Issues & Solutions

### Issue: "No suitable driver found"
**Solution:** Verify the driver is in your JAR:
```bash
jar tf target/data-storage-1.0.0.jar | grep postgresql
jar tf target/data-storage-1.0.0.jar | grep mysql
```

### Issue: "Connection refused"
**Solution:** Check if database is running:
```bash
docker-compose ps
# or
nc -zv localhost 5432
```

### Issue: "Authentication failed"
**Solution:** Verify credentials match your database setup

### Issue: "Database does not exist"
**Solution:** Create the database:
```bash
# PostgreSQL
docker exec -it hiip-postgres createdb -U hiip_user hiipdb

# MySQL
docker exec -it hiip-mysql mysql -u root -proot_pass -e "CREATE DATABASE hiipdb;"
```

---

## Summary

✅ **All database drivers included in JAR**
✅ **No code changes needed**
✅ **No recompilation needed**
✅ **Just set environment variables**
✅ **Switch databases in seconds**

Your application is now **truly database-agnostic**! 🎉
