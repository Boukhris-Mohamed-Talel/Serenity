# Serenity

A Mental healthcare management system built with Angular and Spring Boot. The repository is organized in a **microservice-oriented layout** — see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

- **apps/web-app** — Angular SPA (patients & admins)
- **apps/insurance-portal** — External insurance portal (Node/Express)
- **services/platform-api** — Core backend API (auth, users, insurance)

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
cd services/platform-api
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

### Insurance portal (optional)

```bash
cd apps/insurance-portal
npm install
npm start
```

Runs on **http://localhost:3000**

### Database

The platform API auto-creates the database on first run. Make sure MySQL is running and update credentials in `services/platform-api/src/main/resources/application.yml` if needed.
