# Services

Backend services. **One folder = one deployable service** (own process, port, config).

## Current services

| Service             | Path                        | Port | Description              |
|---------------------|-----------------------------|------|--------------------------|
| **API Gateway**     | `services/API_Gatewya/`     | 8082 | Routes all service requests.  |
| **user-service**    | `services/user-service/`    | 8081 | Auth, user CRUD, profiles. |
| **pharmacy-service** | `services/pharmacy-service/` | 8083 | Pharmacy products, prescriptions. |
| **insurance-service** | `services/insurance-service/` | 8090 | Insurance claims, reimbursements. |
| **marketplace-service** | `services/marketplace-service/` | 8088 | Mental health products, checkout, order history. |

## Run order

1. **user-service** — `cd services/user-service && mvn spring-boot:run`.
2. **insurance-service** — `cd services/insurance-service && mvn spring-boot:run`.
3. **marketplace-service** — `cd services/marketplace-service && mvn spring-boot:run`.

## Database consolidation model

Default configuration is now consolidated into **2 databases**:

- **healthcare_core_db**: user-service + insurance-service data
- **healthcare_commerce_db**: marketplace-service + pharmacy-service data

This reduces operational overhead while still separating identity/claims from commerce data.

You can override names using env variables:

- `CORE_DB_NAME` (default: `healthcare_core_db`)
- `COMMERCE_DB_NAME` (default: `healthcare_commerce_db`)
- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`

For pharmacy's cross-read user integration, `USER_DB_URL` defaults to `CORE_DB_NAME`.

The web-app calls user-service for auth/users and insurance-service for claims (send `X-User-Id` header for the logged-in user id).

## Adding another service

Create a new folder under `services/` (e.g. `notifications-service`), same layout as these two. See [docs/ADDING_A_SERVICE.md](../docs/ADDING_A_SERVICE.md).
