# ARCHITECTURE.md

This file defines the project's layer structure and dependency rules.
The 03-lint.sh validator enforces these rules mechanically.

## Layer Structure

```
┌─────────────────────────────────────────────────────────────┐
│                      Adapter In Layer                       │
│              (adapter.in.web — Controllers, DTOs)           │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│          (application — Use Cases, Port Interfaces)         │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│         (domain — Entities, Value Objects, State Machine)   │
├─────────────────────────────────────────────────────────────┤
│                  Adapter Out Layer                          │
│       (adapter.out.persistence — JPA, External APIs)        │
└─────────────────────────────────────────────────────────────┘
```

## Dependency Direction Rules {#layer-rules}

```
Allowed: adapter.in → application → domain ← adapter.out
Forbidden: domain → application
Forbidden: domain → adapter.out
Forbidden: application → adapter.in
Forbidden: application → adapter.out (concrete classes)
```

**Core principle**: The Domain layer has no outward dependencies.
`adapter.out` implements Port interfaces defined in `application`.

## Package Structure

```
src/main/kotlin/com/demo/tourwave/
├── adapter/
│   ├── in/
│   │   ├── web/<bounded-context>/    # @RestController — HTTP entry point, DTO ↔ Command mapping
│   │   ├── job/                      # Scheduled job entry points (calls application services)
│   │   └── health/                   # Health check endpoints
│   └── out/
│       ├── persistence/<bounded-context>/  # JPA entities, repositories, Port implementations
│       ├── auth/                           # JWT / auth adapter
│       └── payment/                        # External payment adapter
├── application/<bounded-context>/
│   └── port/                         # Port interfaces (Repository, external service contracts)
├── domain/<bounded-context>/         # Entities, value objects, state transitions (no Spring/JPA/Web)
└── bootstrap/                        # Bean assembly (MysqlPersistenceConfig, AuthConfig, UseCaseConfig)

src/main/kotlin/com/demo/tourwaveworker/
└── (WorkerApplication — background job worker, calls application services only)
```

Bounded contexts: `announcement`, `asset`, `auth`, `booking`, `customer`, `inquiry`, `instructor`, `occurrence`, `operations`, `organization`, `participant`, `payment`, `reporting`, `review`, `tour`, `user`

## Rules

- `adapter.in` must not directly inject `adapter.out` concrete classes
- `domain` must not contain Spring, JPA, or Web annotations
- Business logic and transaction boundaries belong only in `application`
- `adapter.out` implements Port interfaces; `application` depends only on Port interfaces
- Worker jobs must not contain domain logic — they call `application` services only

## Validator Error Message Example

```
[ARCHITECTURE VIOLATION] src/domain/service/BookingDomainService.kt:12
  The domain/ layer is directly importing from the adapter.out/ layer.

  Allowed direction: domain ← adapter.out (via Port interfaces)

  How to fix:
  1. Define an interface in application/<bounded-context>/port/
     e.g.: interface BookingRepository { fun findById(id: Long): Booking? }
  2. Implement the interface in adapter.out/persistence/<bounded-context>/
  3. In application/, depend only on the Port interface
```

## Cross-Cutting Concerns

Cross-cutting concerns that span layers are handled through **Port interfaces**:
- Auth (JWT — `AuthConfig` in bootstrap)
- Metrics (Micrometer + Prometheus)
- Persistence profile switching (`mysql` / `mysql-test` profiles via `MysqlPersistenceConfig`)
- Idempotency (`Idempotency-Key` header — enforced at `adapter.in` layer)
