# Apps

User-facing applications. **One folder = one deployable app** (own build, own port).

## Current apps

| App        | Path                | Port | Description              |
|------------|---------------------|------|--------------------------|
| **web-app** | `apps/web-app/`     | 4200 | Angular SPA (patients, admins). Talks to `services/user-service` and, when present, other services. |

## Adding a new app

1. **Create a new folder** under `apps/`, e.g. `insurance-portal`, `mobile-web`, `admin-console`.
2. **Own build and config**: own `package.json` / `pom.xml`, own env or config. No sharing of build artifacts between apps.
3. **Own port**: each app runs on its own port (e.g. 4200, 4201, 3000).
4. **Calls services**: apps call `services/*` via HTTP; they do not call each other directly.

## Target layout

```
apps/
├── web-app/           # Main Serenity UI (Angular)
├── insurance-portal/  # External insurer UI (Node/Express) — when added
└── README.md          # this file
```
