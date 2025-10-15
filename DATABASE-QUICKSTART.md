# Quick Start: Testing Different Databases

This guide shows how to quickly test HIIP with different database systems.

## Prerequisites

- Docker and Docker Compose installed
- Maven installed
- Java 17+ installed

## Step 1: Add Database Drivers (Optional for Basic Testing)

Your application already works with H2. To test other databases, you can add their drivers:

### Option A: Add All Major Database Drivers

Edit `pom.xml` and add before the `</dependencies>` closing tag:

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

<!-- SQL Server -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
```

Then rebuild:
```bash
mvn clean package -DskipTests
```

## Step 2: Start Database Containers

### Start All Databases
```bash
docker-compose up -d
```

### Or Start Individual Databases

**PostgreSQL only:**
```bash
docker-compose up -d postgres
```

**MySQL only:**
```bash
docker-compose up -d mysql
```

**MariaDB only:**
```bash
docker-compose up -d mariadb
```

**SQL Server only:**
```bash
docker-compose up -d sqlserver
```

## Step 3: Configure Your Application

### Option A: Using Environment Variables

**For PostgreSQL:**
```bash
export HIIP_DATASOURCE_URL="jdbc:postgresql://localhost:5432/hiipdb"
export HIIP_DATASOURCE_DRIVER="org.postgresql.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
```

**For MySQL:**
```bash
export HIIP_DATASOURCE_URL="jdbc:mysql://localhost:3306/hiipdb?useSSL=false&serverTimezone=UTC"
export HIIP_DATASOURCE_DRIVER="com.mysql.cj.jdbc.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"
```

**For SQL Server:**
```bash
export HIIP_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true"
export HIIP_DATASOURCE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver"
export HIIP_DATASOURCE_USERNAME="sa"
export HIIP_DATASOURCE_PASSWORD="HiipPass123!"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.SQLServerDialect"
```

### Option B: Using .env File

1. Copy the example:
```bash
cp .env.example .env
```

2. Edit `.env` and uncomment the database configuration you want to use

3. Load the environment:
```bash
set -a
source .env
set +a
```

## Step 4: Start Your Application

```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/data-storage-1.0.0.jar
```

## Step 5: Verify Connection

Check the application logs for successful database connection:
```
INFO  HikariDataSource - HikariPool-1 - Starting...
INFO  HikariPool - HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
INFO  HikariDataSource - HikariPool-1 - Start completed.
```

Access the application:
```
http://localhost:8080
```

## Database Management Tools

### PostgreSQL - pgAdmin
```
http://localhost:5050
Email: admin@hiip.com
Password: admin
```

Add server connection:
- Host: postgres (or localhost if connecting from host)
- Port: 5432
- Username: hiip_user
- Password: hiip_pass

### MySQL/MariaDB - phpMyAdmin
```
http://localhost:8081
Username: hiip_user
Password: hiip_pass
```

### SQL Server - Azure Data Studio or SSMS
```
Server: localhost,1433
Username: sa
Password: HiipPass123!
```

## Testing Different Databases

### Test with H2 (Default - No Docker Needed)
```bash
# No environment variables needed
mvn spring-boot:run
```

### Test with PostgreSQL
```bash
docker-compose up -d postgres
export HIIP_DATASOURCE_URL="jdbc:postgresql://localhost:5432/hiipdb"
export HIIP_DATASOURCE_DRIVER="org.postgresql.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
mvn spring-boot:run
```

### Test with MySQL
```bash
docker-compose up -d mysql
export HIIP_DATASOURCE_URL="jdbc:mysql://localhost:3306/hiipdb?useSSL=false&serverTimezone=UTC"
export HIIP_DATASOURCE_DRIVER="com.mysql.cj.jdbc.Driver"
export HIIP_DATASOURCE_USERNAME="hiip_user"
export HIIP_DATASOURCE_PASSWORD="hiip_pass"
export HIIP_HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"
mvn spring-boot:run
```

## Create Database Manually (if needed)

Some databases auto-create the database from Docker environment, but you can also create manually:

### PostgreSQL
```bash
docker exec -it hiip-postgres psql -U hiip_user -d postgres -c "CREATE DATABASE hiipdb;"
```

### MySQL
```bash
docker exec -it hiip-mysql mysql -u root -proot_pass -e "CREATE DATABASE IF NOT EXISTS hiipdb;"
```

### SQL Server
```bash
docker exec -it hiip-sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "HiipPass123!" -Q "CREATE DATABASE hiipdb;"
```

## Cleanup

### Stop All Databases
```bash
docker-compose down
```

### Stop and Remove All Data
```bash
docker-compose down -v
```

### Stop Individual Database
```bash
docker-compose stop postgres
docker-compose stop mysql
docker-compose stop mariadb
docker-compose stop sqlserver
```

## Troubleshooting

### Connection Refused
- Check if container is running: `docker-compose ps`
- Check container logs: `docker-compose logs postgres`
- Verify port is not in use: `lsof -i :5432`

### Database Does Not Exist
- Wait a few seconds after starting container
- Create database manually (see above)
- Check container logs for initialization errors

### Authentication Failed
- Verify credentials match docker-compose.yml
- For SQL Server, ensure password meets complexity requirements
- Check if user has been created properly

### Hibernate Errors
- Verify the dialect matches the database
- Check if JDBC driver is in classpath
- Review SQL in logs: `LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG`

## Performance Testing

Compare performance across databases:

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Test with different databases
ab -n 1000 -c 10 http://localhost:8080/api/v1/data
```

## Migration Testing

Test data migration between databases:

1. Start with H2 and create test data
2. Stop application
3. Export data (if needed)
4. Start PostgreSQL container
5. Configure PostgreSQL
6. Restart application
7. Verify data (note: H2 data won't auto-migrate)

## Next Steps

1. Choose your production database (PostgreSQL recommended)
2. Set up proper backup strategy
3. Configure connection pooling
4. Add monitoring and metrics
5. Implement Flyway/Liquibase for schema migrations
6. Set up database replication for high availability

## Reference

- PostgreSQL: https://www.postgresql.org/docs/
- MySQL: https://dev.mysql.com/doc/
- SQL Server: https://docs.microsoft.com/en-us/sql/
- Hibernate: https://hibernate.org/orm/documentation/
