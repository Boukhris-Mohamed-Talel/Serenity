# Services

Backend APIs and microservices. **One folder = one deployable service** (own process, own port, own config).

## Current services

| Service          | Path               | Port | Description                    |
|------------------|--------------------|------|--------------------------------|
| **user-service** | `services/user-service/` | 8081 | Auth, user CRUD, profiles. |

## Adding a new microservice

1. **Create a new folder** under `services/` with a clear name, e.g. `insurance-service`.
2. **One service per folder**: own `pom.xml` (or build file), own `application.yml`, own main class. Do not put multiple runnable apps in one folder.
3. **Own port**: assign a dedicated port (e.g. 8082, 8083) in that service’s config.
4. **Communication**: services call each other via HTTP (REST) or message queue when you introduce one. No shared database per service when you go full microservices; for now, new services can share the same DB or use their own.
5. **Convention**: same repo layout as `user-service` (e.g. `src/main/java`, `src/main/resources`) so the repo stays consistent.

## Target layout (when more services are added)

```
services/
├── user-service/       # Auth, user CRUD, profiles (current)
├── insurance-service/  # Claims, reimbursements (to be added)
└── README.md           # this file
```
