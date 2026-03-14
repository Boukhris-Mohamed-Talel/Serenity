# Serenity — Microservice-oriented architecture

This repository is organized to reflect a **microservice-style layout**: applications and backend services live under clear top-level namespaces. The codebase can evolve toward multiple deployable services without a big reshuffle.

## Repository layout (arborescence)

```
healthcare-system/
├── apps/                          # Frontends & user-facing applications
│   ├── web-app/                   # Angular SPA (patients, admins)
│   └── insurance-portal/          # External insurance company portal (Node/Express)
│
├── services/                      # Backend services (APIs)
│   └── platform-api/             # Core platform API (auth, users, insurance)
│
├── docs/                         # Architecture and project docs
│   └── ARCHITECTURE.md
├── README.md
└── .gitignore
```

## Rationale

| Directory   | Role |
|------------|------|
| **apps/**  | All runnable “front-end” or external apps. Each app can have its own stack and deploy pipeline. |
| **services/** | All runnable backend services. Today there is one API (`platform-api`); later you can add e.g. `auth-service`, `insurance-service` without changing the overall tree. |
| **docs/**   | Central place for architecture and high-level documentation. |

## Current vs future

- **Today:** One backend service (`platform-api`) handles auth, users, and insurance. Two apps: `web-app` (Angular) and `insurance-portal` (Node).
- **Later:** You can add new entries under `services/` (e.g. `services/insurance-service/`) and under `apps/` without reworking the rest of the tree. Shared contracts or libs can go in a top-level `shared/` or inside each service as needed.

## Run order

1. **Database** — MySQL (or Postgres) up and database created.
2. **services/platform-api** — `mvn spring-boot:run` (e.g. port 8081).
3. **apps/web-app** — `npm install && ng serve` (e.g. port 4200).
4. **apps/insurance-portal** (optional) — `npm start` (e.g. port 3000).

See [README.md](../README.md) for exact commands and prerequisites.
