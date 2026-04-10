# safepoint-api

> Spring Boot REST API for SafePoint — orchestrates the mental health risk assessment pipeline combining validated PHQ-9/GAD-7 scoring with DistilBERT ML classification, anonymous user identity, and safety plan storage.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL 16 |
| Security | Spring Security (stateless, BCrypt) |
| HTTP Client | RestTemplate |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Build | Maven |

---

## Architecture

This service is the central orchestrator in a three-service architecture:

```
React Frontend (port 5173)
        │  Axios / REST
        ▼
Spring Boot API (port 8080)   ◄──► PostgreSQL 16 (port 5432)
        │  HTTP (internal only)
        ▼
Python ML Service (port 8001)
```

---

## Risk Assessment Algorithm

The core business logic combines two independent signals into a final risk level:

1. **Questionnaire scoring** — PHQ-9 and GAD-7 scored against validated clinical cutoffs
2. **ML text classification** — DistilBERT model via internal Python service

```
finalRisk = max(questionnaireRisk, mlRisk)
```

**PHQ-9 thresholds:** score ≥ 10 → MEDIUM · score ≥ 20 → HIGH  
**GAD-7 thresholds:** score ≥ 10 → MEDIUM · score ≥ 15 → HIGH  
**ML quality gate:** text must have ≥ 15 words and ≥ 8 unique words to be sent to ML  
**ML confidence threshold:** ML signal ignored if confidence < 0.60  

---

## Project Structure

```
com.safepoint.api
├── controller/           # REST endpoints
│   ├── AnalysisController        POST /api/v1/analysis
│   ├── AuthController            POST /api/v1/auth/register · verify
│   ├── SafetyPlanController      GET · POST · DELETE /api/v1/safety-plan
│   ├── WellbeingResourceController  GET /api/v1/wellbeing
│   └── ResourceController        GET /api/v1/facilities
├── service/              # Business logic
│   ├── AnalysisService           Risk pipeline orchestration
│   ├── MlService                 HTTP client for Python ML service
│   ├── TranslationService        MyMemory API (ES → EN)
│   ├── SamhsaService             SAMHSA FindTreatment.gov proxy
│   ├── SafetyPlanService         SHA-256 anonymous identity
│   ├── AnonymousUserService      BCrypt PIN hashing, username generation
│   └── WellbeingResourceService  BART excerpt refresh (@Scheduled weekly)
├── model/
│   ├── entity/           # JPA entities (AnonymousUser, SafetyPlan, WellbeingResource)
│   ├── AnalysisRequest   # Request DTO
│   └── AnalysisResponse  # Response DTO with MlAnalysisResult
├── repository/           # Spring Data JPA repositories
├── dto/                  # Auth and SafetyPlan DTOs
└── config/               # Security, RestTemplate beans
```

---

## API Endpoints

### Analysis
```
POST  /api/v1/analysis          Run full risk assessment (PHQ-9 + GAD-7 + optional AI text)
```

### Authentication
```
POST  /api/v1/auth/register     Register anonymous user → returns username (e.g. blue-river-42)
POST  /api/v1/auth/verify       Verify username + PIN
```

### Safety Plan
```
GET   /api/v1/safety-plan       Retrieve safety plan by username + PIN
POST  /api/v1/safety-plan       Save or update safety plan
DELETE /api/v1/safety-plan      Delete safety plan
```

### Wellbeing Resources
```
GET   /api/v1/wellbeing                      Get all resources with AI-generated excerpts
POST  /api/v1/wellbeing/{id}/refresh-excerpt  Manually refresh excerpt for one resource
```

### Facilities
```
GET   /api/v1/facilities        SAMHSA treatment facility search (location + insurance filter)
```

Full interactive documentation available at `http://localhost:8080/swagger-ui.html` after startup.

---

## Privacy Design

SafePoint stores no personally identifiable information:

- **Anonymous identity** — human-readable username (`blue-river-42`) + 4-digit PIN
- **PIN storage** — BCrypt hash only, never the raw PIN
- **Safety plan linkage** — SHA-256(username + ":" + pin), not reversible
- **Free text** — processed in memory by ML service, never logged or persisted
- **Assessment history** — browser localStorage only, never sent to server

---

## Configuration

All sensitive values are loaded from environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/safepoint
    username: ${DB_USER:safepoint}
    password: ${DB_PASSWORD:safepoint}

ml:
  service:
    url: ${ML_SERVICE_URL:http://127.0.0.1:8001}
    timeout-ms: 10000

samhsa:
  api:
    url: https://findtreatment.samhsa.gov/locator/row
    key: ${SAMHSA_API_KEY:}
```

---

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 16 (or Docker/Podman)

### Start database
```bash
podman compose -f infra/docker-compose.yml up -d
```

### Start API
```bash
./mvnw spring-boot:run
```

### Run tests
```bash
./mvnw test
```

---

## Related Repositories

| Repository | Description |
|---|---|
| [safepoint-ui](https://github.com/asihub/safepoint-ui) | React 19 frontend |
| [safepoint-ml](https://github.com/asihub/safepoint-ml) | Python ML service (DistilBERT + BART) |
