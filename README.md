# hiip
A simple universal data storage.

## Overview

Hiip is a Spring Boot application that provides a REST API for storing data with tags. Each user can store, search, and manage their own data with tag-based search functionality.

## Features

- **REST API** for data storage with tags
- **Tag-based search** - search data by one or multiple tags
- **Soft delete** - data is hidden, not permanently removed
- **Flat structure** - no hierarchy between data entries
- **Authentication** - user-based data ownership
- **H2 Database** - in-memory database for data storage

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Building the Application

```bash
mvn clean package
```

### Running the Application

```bash
java -jar target/data-storage-1.0.0.jar
```

The application will start on `http://localhost:8080`.

### Default Test Users

Two test users are created automatically:
- Username: `user1`, Password: `password1`
- Username: `user2`, Password: `password2`

## API Endpoints

All endpoints require HTTP Basic Authentication.

### Create Data

```bash
POST /api/data
Content-Type: application/json

{
  "content": "Your data content",
  "tags": ["tag1", "tag2"]
}
```

Example:
```bash
curl -u user1:password1 -X POST http://localhost:8080/api/data \
  -H "Content-Type: application/json" \
  -d '{"content":"My data","tags":["important","work"]}'
```

### Get All Data

```bash
GET /api/data
```

Example:
```bash
curl -u user1:password1 http://localhost:8080/api/data
```

### Get Data by ID

```bash
GET /api/data/{id}
```

Example:
```bash
curl -u user1:password1 http://localhost:8080/api/data/1
```

### Search Data by Tags

```bash
GET /api/data/search?tags=tag1&tags=tag2
```

Example:
```bash
curl -u user1:password1 "http://localhost:8080/api/data/search?tags=important"
```

### Update Data

```bash
PUT /api/data/{id}
Content-Type: application/json

{
  "content": "Updated content",
  "tags": ["updated", "tags"]
}
```

Example:
```bash
curl -u user1:password1 -X PUT http://localhost:8080/api/data/1 \
  -H "Content-Type: application/json" \
  -d '{"content":"Updated data","tags":["updated"]}'
```

### Hide Data (Soft Delete)

```bash
DELETE /api/data/{id}
```

Example:
```bash
curl -u user1:password1 -X DELETE http://localhost:8080/api/data/1
```

## H2 Console

The H2 database console is available for development and testing:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:hiipdb`
- Username: `sa`
- Password: (leave empty)

## Technical Details

- **Framework**: Spring Boot 3.1.5
- **Database**: H2 (in-memory)
- **Security**: Spring Security with HTTP Basic Authentication
- **ORM**: Hibernate/JPA
- **Build Tool**: Maven

## License

Licensed under the Apache License, Version 2.0.

