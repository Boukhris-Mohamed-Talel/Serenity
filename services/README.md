# Services

Backend services. **One folder = one deployable service** (own process, port, config).

## Current services

| Service | Path | Port | Description |
|---------|------|------|-------------|
| **user-service** | `services/user-service/` | 8081 | Auth, user CRUD, profiles. |
| **API_Gatewya** | `services/API_Gatewya/` | 8082 | Spring Cloud Gateway; routes `/api/**` to user-service and appointment-service. |
| **appointment-service** | `services/appointment-service/` | 8091 | Appointments, calendar, notifications. |

## Run order

1. **user-service** — `cd services/user-service && mvn spring-boot:run`.
2. **appointment-service** — `cd services/appointment-service && mvn spring-boot:run`.
3. **API_Gatewya** — `cd services/API_Gatewya && mvn spring-boot:run`.

The Angular app points at the gateway (e.g. `http://localhost:8082/api`). Services can share the same MySQL database (`healthcare_db`) where configured.

## Adding another service

Create a new folder under `services/` (e.g. `notifications-service`), same layout as an existing service. See [docs/ADDING_A_SERVICE.md](../docs/ADDING_A_SERVICE.md).
