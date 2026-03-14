# Serenity

A Mental healthcare management system built with Angular and Spring Boot. The repository is organized in a **microservice-oriented layout** — see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout (microservice-ready)

- **apps/** — User-facing apps. One folder = one app. See [apps/README.md](apps/README.md).
- **services/** — Backend services. One folder = one service. See [services/README.md](services/README.md).
- **docs/** — [ARCHITECTURE.md](docs/ARCHITECTURE.md), [ADDING_A_SERVICE.md](docs/ADDING_A_SERVICE.md).

Current: **apps/web-app** (Angular SPA), **services/user-service** (auth, users). New services (e.g. insurance) go as **sibling folders** under `services/`.

## Tech Stack

- **Frontend:** Angular 17, SCSS, Reactive Forms
- **Backend:** Java 17, Spring Boot 3.2, Spring Security, JWT
- **Database:** MySQL 8
- **Tooling:** MapStruct, Lombok, Maven

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ & npm 9+
- MySQL 8.0+
- Angular CLI — `npm install -g @angular/cli`

### Platform API (backend)

```bash
cd services/user-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8081**

### Web app (frontend)

```bash
cd apps/web-app
npm install
ng serve
```

Runs on **http://localhost:4200**

### Database

The platform API auto-creates the database on first run. Make sure MySQL is running and update credentials in `services/user-service/src/main/resources/application.yml` if needed.
