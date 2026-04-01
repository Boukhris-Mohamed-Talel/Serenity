# Serenity

A mental healthcare management system built with Angular and Spring Boot. The repo is **microservice-oriented**: each backend domain lives in its own service under `services/`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

- **apps/web-app** — Angular SPA (patients & admins). Calls the API gateway (routes to user-service and appointment-service).
- **services/user-service** — Auth, user CRUD, profiles (port **8081**).
- **services/API_Gatewya** — Spring Cloud Gateway, single entry for the SPA (port **8082**).
- **services/appointment-service** — Appointments and notifications (port **8091**).

## Tech stack

- **Frontend:** Angular 17, SCSS, Reactive Forms
- **Backend:** Java 17, Spring Boot 3.2 (user-service, appointment-service, gateway)
- **Database:** MySQL 8 (shared where configured)
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

### 3. Appointment service

```bash
cd services/appointment-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8091** (see its `application.yml` if different).

### 4. API gateway

```bash
cd services/API_Gatewya
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8082** — this is the base URL used by the web app (`environment.apiUrl`).

### 5. Web app

```bash
cd apps/web-app
npm install
ng serve
```

Runs on **http://localhost:4200**. Ensure the gateway is up so `/api/**` requests are routed correctly.
