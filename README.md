# Serenity

A Mental healthcare management system built with Angular and Spring Boot.

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

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8081**

### Frontend

```bash
cd frontend
npm install
ng serve
```

Runs on **http://localhost:4200**

### Database

The app auto-creates the database on first run. Make sure MySQL is running and update credentials in `backend/src/main/resources/application.yml` if needed.
