# Serenity

A mental healthcare management system built with Angular and Spring Boot. The repo is **microservice-oriented**: each backend domain lives in its own service under `services/`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

- **apps/web-app** — Angular SPA (patients & admins). Calls the **API gateway** on port **8082**, which routes to user-service, appointment-service, insurance-service, and other backends.
- **services/user-service** — Auth, user CRUD, profiles (port **8081**).
- **services/API_Gatewya** — Spring Cloud Gateway, single HTTP entry for the SPA (port **8082**).
- **services/appointment-service** — Appointments and notifications (port **8091**).
- **services/insurance-service** — Insurance claims and reimbursements (see that service’s `application.yml` for its port).

Additional domain services (pharmacy, marketplace, monitoring, etc.) live under `services/` as separate modules—see `services/README.md`.

## Tech stack

- **Frontend:** Angular 17, SCSS, Reactive Forms
- **Backend:** Java 17, Spring Boot 3.2 (user-service, appointment-service, gateway, and other services)
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

### 4. Insurance service (if you use claims UI)

```bash
cd services/insurance-service
mvn clean install
mvn spring-boot:run
```

### 5. API gateway

```bash
cd services/API_Gatewya
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8082** — this is the base URL used by the web app (`environment.apiUrl`).

### 6. Web app

```bash
cd apps/web-app
npm install
ng serve
```

Runs on **http://localhost:4200**. Ensure the gateway and the backends you need are running so `/api/**` requests succeed.
