# Employee Management System

A robust and secure employee management system built with Spring Boot that provides REST APIs for managing employees, departments, users, roles, and audit trails.

## Technology Stack

- Java 17
- Spring Boot 3.1.2
- Spring Security with JWT Authentication
- MySQL Database
- Maven
- Swagger UI (OpenAPI 3.0)
- ModelMapper
- Lombok

## Features

- User authentication and authorization using JWT tokens
- Role-based access control (ADMIN, USER)
- CRUD operations for employees, departments, users
- Role management system
- Audit system with approval workflows
- API documentation with Swagger UI

## Getting Started

### Prerequisites

- JDK 17
- MySQL Server
- Maven

### Configuration

1. Clone the repository:
```bash
git clone <repository-url>
```

2. Configure MySQL database in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee
spring.datasource.username=root
spring.datasource.password=your password
```

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

Access Swagger UI documentation at: `http://localhost:8080/swagger-ui.html`

### Authentication APIs

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "string",
  "username": "string",
  "email": "string",
  "password": "string"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "string",
  "password": "string"
}
```

### Employee APIs

#### Create Employee (ADMIN only)
```http
POST /api/employees
Authorization: Bearer <token>
Content-Type: application/json

{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "departmentId": 0
}
```

#### Get All Employees
```http
GET /api/employees
Authorization: Bearer <token>
```

#### Get Employee by ID
```http
GET /api/employees/{id}
Authorization: Bearer <token>
```

#### Update Employee (ADMIN only)
```http
PUT /api/employees/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "departmentId": 0
}
```

#### Delete Employee (ADMIN only)
```http
DELETE /api/employees/{id}
Authorization: Bearer <token>
```

### Department APIs

#### Create Department (ADMIN only)
```http
POST /api/departments
Authorization: Bearer <token>
Content-Type: application/json

{
  "departmentName": "string",
  "departmentDescription": "string"
}
```

#### Get All Departments
```http
GET /api/departments
Authorization: Bearer <token>
```

### Role Management APIs

#### Create Role
```http
POST /api/role
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "string"
}
```

#### Assign Role to User
```http
POST /api/role/setRole
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": 0,
  "roles": [
    {
      "id": 0,
      "name": "string"
    }
  ]
}
```

### Audit APIs

#### Create Audit
```http
POST /api/audit/create
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "string",
  "content": "string",
  "amountMoney": 0,
  "auditType": 0,
  "requireAllApprovalPassing": true,
  "requirePeerReview": true,
  "allowedToLeapfrog": true
}
```

## Security

The application uses JWT (JSON Web Token) for authentication. Include the JWT token in the Authorization header for protected endpoints:

```http
Authorization: Bearer <token>
```

## Role Hierarchy

- ADMIN: Full access to all APIs
- USER: Limited access to view operations
- Custom roles can be created and assigned

## Error Handling

The application provides detailed error responses:

```json
{
  "timeStamp": "2023-08-10T10:00:00",
  "message": "Error message",
  "path": "Request path",
  "errorCode": "Error code"
}
```

## Development

1. For local development, use the included Swagger UI
2. Test endpoints with valid JWT tokens
3. Check role permissions before accessing protected endpoints

## License

This project is licensed under the MIT License - see the LICENSE file for details
