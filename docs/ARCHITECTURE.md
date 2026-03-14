# Serenity — Microservice-oriented architecture

The repo is organized so each backend domain is a **separate service** under `services/`. One folder = one deployable API.

## Repository layout

```
healthcare-system/
├── apps/
│   └── web-app/                 # Angular SPA (calls user-service + insurance-service)
│
├── services/
│   ├── user-service/            # Auth, user CRUD, profiles (port 8081)
│   ├── insurance-service/      # Claims, reimbursements (port 8082)
│   └── README.md
│
├── docs/
│   ├── ARCHITECTURE.md
│   └── ADDING_A_SERVICE.md
└── README.md
```

## Services

| Service             | Port | Role |
|---------------------|------|------|
| **user-service**    | 8081 | Login, register, user management, profiles. JWT auth. |
| **insurance-service** | 8082 | Submit claims, list claims, approve/reject. Uses `X-User-Id` header for caller identity. |

The web-app talks to both: user-service for auth and users, insurance-service for insurance endpoints (and passes the logged-in user id in `X-User-Id`).

## Run order

1. MySQL up, database created.
2. `cd services/user-service && mvn spring-boot:run`
3. `cd services/insurance-service && mvn spring-boot:run`
4. `cd apps/web-app && npm install && ng serve`

See [README.md](../README.md) and [services/README.md](../services/README.md) for details.
