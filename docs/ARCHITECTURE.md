# Serenity — Microservice-oriented architecture

The repo is organized so each backend domain is a **separate service** under `services/`. One folder = one deployable API.

## Repository layout

```
healthcare-system/
├── apps/
│   └── web-app/                 # Angular SPA (calls API gateway)
│
├── services/
│   ├── user-service/            # Auth, user CRUD, profiles (port 8081)
│   ├── API_Gatewya/             # Spring Cloud Gateway (port 8082)
│   ├── appointment-service/     # Appointments (port 8091)
│   └── README.md
│
├── docs/
│   ├── ARCHITECTURE.md
│   └── ADDING_A_SERVICE.md
└── README.md
```

## Services

| Service | Port | Role |
|---------|------|------|
| **user-service** | 8081 | Login, register, user management, profiles. JWT auth. |
| **API_Gatewya** | 8082 | Routes `/api/auth/**`, `/api/users/**`, `/api/appointments/**` to the appropriate backends. |
| **appointment-service** | 8091 | Appointments, notifications. Uses JWT from the gateway. |

The web-app uses the gateway as its API base URL; the gateway forwards requests and can attach user context headers.

## Run order

1. MySQL up, database created.
2. `cd services/user-service && mvn spring-boot:run`
3. `cd services/appointment-service && mvn spring-boot:run`
4. `cd services/API_Gatewya && mvn spring-boot:run`
5. `cd apps/web-app && npm install && ng serve`

See [README.md](../README.md) and [services/README.md](../services/README.md) for details.
