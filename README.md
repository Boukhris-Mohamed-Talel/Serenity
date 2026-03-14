# Serenity

A mental healthcare management system built with Angular and Spring Boot. The repo is **microservice-oriented**: each backend domain lives in its own service under `services/`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

- **apps/web-app** — Angular SPA (patients & admins). Calls user-service and insurance-service.
- **services/user-service** — Auth, user CRUD, profiles (port **8081**).
- **services/insurance-service** — Insurance claims and reimbursements (port **8082**).

## Tech stack

- **Frontend:** Angular 17, SCSS, Reactive Forms
- **Backend:** Java 17, Spring Boot 3.2 (user-service: Security, JWT; insurance-service: REST API)
- **Database:** MySQL 8 (shared by both services)
- **Tooling:** MapStruct, Lombok, Maven

## Getting started

### Prerequisites

- Java 17+, Maven 3.8+, Node.js 18+, MySQL 8.0+
- Angular CLI: `npm install -g @angular/cli`

### 1. Database

Create and run MySQL (e.g. `healthcare_db`). Configure credentials in each service’s `application.yml` if needed.

### 2. User service (auth, users)

```bash
cd services/user-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8081**

### 3. Insurance service (claims)

```bash
cd services/insurance-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8082**

### 4. Web app

```bash
cd apps/web-app
npm install
ng serve
```

Runs on **http://localhost:4200**. Set its API base URLs to user-service (8081) and insurance-service (8082); for insurance endpoints send the logged-in user id in `X-User-Id` (from the auth response).
