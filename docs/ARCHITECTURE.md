# Serenity — Architecture (microservice-ready)

The repo is laid out so we can add real microservices later without reshuffling. Right now there is **one** backend service and **one** main app; the **tree** is already microservice-style.

## Repository layout

```text
healthcare-system/
├── apps/                    # User-facing applications (one folder = one app)
│   ├── web-app/             # Angular SPA — main Serenity UI
│   └── README.md
│
├── services/                # Backend services (one folder = one service)
│   ├── user-service/        # Auth, user CRUD, profiles
│   └── README.md
│
├── docs/
│   ├── ARCHITECTURE.md      # this file
│   └── ADDING_A_SERVICE.md  # how to add a new microservice
│
└── README.md
```

## Current vs target

| Layer     | Now                         | Target (when services are added)        |
|----------|-----------------------------|-----------------------------------------|
| **Apps** | `web-app` only              | `web-app` + e.g. `insurance-portal`      |
| **Services** | `user-service` (auth + users) | `user-service` + `insurance-service` (and others as needed) |
| **Communication** | N/A (one API)          | Apps → services via HTTP; services → each other via HTTP (or events later) |

## Principles

1. **One folder under `apps/` = one deployable frontend/app.** No second app hidden inside `web-app`.
2. **One folder under `services/` = one deployable backend service.** `user-service` is auth + users. New domains (e.g. insurance) become **new folders** under `services/`, not packages inside `user-service`.
3. **Same repo, clear boundaries.** Each service has its own code, config, and port; when you add a domain, create a new `services/<name>/` and call it via HTTP.
4. **Docs and READMEs** under `services/` and `docs/` describe the layout and how to add a service so the repo is ready when you start adding more.

## See also

- **services/README.md** — What lives under `services/`, current services, and how to add one.
- **apps/README.md** — What lives under `apps/` and how to add an app.
- **docs/ADDING_A_SERVICE.md** — Step-by-step for adding a new microservice.
