# Services

Backend services. **One folder = one deployable service** (own process, port, config).

## Current services

| Service             | Path                        | Port | Description              |
|---------------------|-----------------------------|------|--------------------------|
| **user-service**    | `services/user-service/`    | 8081 | Auth, user CRUD, profiles. |
| **insurance-service** | `services/insurance-service/` | 8082 | Insurance claims, reimbursements. |
| **marketplace-service** | `services/marketplace-service/` | 8085 | Mental health products, checkout, order history. |

## Run order

1. **user-service** — `cd services/user-service && mvn spring-boot:run`.
2. **insurance-service** — `cd services/insurance-service && mvn spring-boot:run`.
3. **marketplace-service** — `cd services/marketplace-service && mvn spring-boot:run`.

Both can share the same MySQL database (`healthcare_db`); insurance-service uses tables `insurance_claims`, `claim_files`, `remboursements`.  
The web-app calls user-service for auth/users and insurance-service for claims (send `X-User-Id` header for the logged-in user id).

## Adding another service

Create a new folder under `services/` (e.g. `notifications-service`), same layout as these two. See [docs/ADDING_A_SERVICE.md](../docs/ADDING_A_SERVICE.md).
