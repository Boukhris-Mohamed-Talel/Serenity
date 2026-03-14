# Adding a new microservice

This repo is structured so new backend services are **siblings** of `user-service`. Use this when you add e.g. `insurance-service` or other domain services.

## 1. Create the service folder

```text
services/
  user-service/      # existing
  insurance-service/ # new — one folder per service
```

## 2. Bootstrap the new service

**Option A – Copy from user-service (Spring Boot)**

- Copy `services/user-service` to `services/<your-service-name>`.
- In the new folder:
  - Change `artifactId` and `name` in `pom.xml` (e.g. `insurance-service`).
  - Change `server.port` in `application.yml` (e.g. 8082).
  - Change main class package/name if you want (e.g. `InsuranceServiceApplication`).
  - Remove everything that does not belong to this domain (e.g. keep only insurance-related controllers, entities, repos, services; delete auth/user-specific code).
  - Adjust `application.yml` (DB, etc.) for this service (own DB or shared, depending on your strategy).

**Option B – New stack (e.g. Node, Go)**

- Create `services/<your-service-name>/` with its own build file and structure.
- Use a dedicated port and document it in `services/README.md`.
- Expose a clear API (REST or other) so other services or the web-app can call it.

## 3. Document the service

- Add a row for the new service in `services/README.md` (table: name, path, port, short description).
- If other services or the web-app call it, document the base URL (e.g. `http://localhost:8082`) and main endpoints (in README or `docs/`).

## 4. Call it from the web-app or other services

- In the Angular app, add (or reuse) an env/config for the new service base URL and call it via `HttpClient`.
- From another service (e.g. `user-service`), use `RestTemplate`, `WebClient`, or an HTTP client to call the new service’s API. No shared in-process code between services.

## Rules of thumb

- **One folder under `services/` = one runnable process.** No “mini-services” as subfolders inside `user-service`.
- **Own port, own config.** Each service has its own `application.yml` (or equivalent) and port.
- **HTTP (or messages) between services.** No direct DB access from one service into another’s DB when you go full microservices.
- **Domain-owned data.** When you add e.g. insurance, put claims/reimbursements in `insurance-service`; `user-service` and apps call it via its API.
