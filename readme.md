# Safe Point API

REST API for managing safety plans, analysis data, and wellbeing resources.

## 🚀 Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Lombok
- OpenAPI / Swagger

---

## 📦 Features

- Anonymous user authentication
- Analysis data collection and retrieval
- Safety plan management (CRUD)
- Wellbeing and general resources
- Secured REST API

---

## 📁 Project Structure

```
com.safepoint.api
├── controller        # REST controllers
├── service           # Business logic
├── repository        # JPA repositories
├── model
├── entity            # JPA entities
├── dto               # Data Transfer Objects
├── config            # Configuration (security, etc.)
```

---

## 🔌 API Endpoints

### Authentication

```
POST   /api/v1/auth      # Create anonymous user
DELETE /api/v1/auth      # Delete user
```

### Analysis

```
POST /api/v1/analysis
GET  /api/v1/analysis/{id}
```

### Safety Plans

```
GET    /api/v1/safety-plans
POST   /api/v1/safety-plans
PUT    /api/v1/safety-plans/{id}
DELETE /api/v1/safety-plans/{id}
```

### Resources

```
GET /api/v1/resources
```

### Wellbeing Resources

```
GET /api/v1/wellbeing-resources
```

---

## 📖 API Documentation

Swagger UI is available after startup:

```
http://localhost:8080/swagger-ui.html
```

---

## ⚙️ Configuration

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/safepoint
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## ▶️ Running the Application

### Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Using Maven

```bash
mvn clean install
java -jar target/safe-point-api-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Running Tests

```bash
mvn test
```

---

## 🔐 Security

- Powered by Spring Security
- Anonymous authentication supported
- Extendable to JWT-based authentication

---

## 🐳 Docker (Optional)

```dockerfile
FROM eclipse-temurin:21-jdk
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## 📌 Future Improvements

- JWT authentication
- Role-based access control
- Audit logging
- Rate limiting
- Docker Compose setup
- CI/CD pipeline

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---

## 📄 License

To be defined