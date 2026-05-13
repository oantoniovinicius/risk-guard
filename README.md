# Risk Guard

An event-driven antifraud platform built with **Spring Boot**, **RabbitMQ**, and **PostgreSQL**. The system processes financial transfers through a real-time risk scoring pipeline, routes flagged transactions to human analysts, and manages the full lifecycle from user onboarding to fund settlement.

Built as a learning project to explore event-driven architecture, messaging systems, containerization, and integration testing in a realistic financial domain.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Domain Modules](#domain-modules)
- [Event-Driven Pipeline](#event-driven-pipeline)
- [Risk Scoring Engine](#risk-scoring-engine)
- [Transaction Lifecycle](#transaction-lifecycle)
- [API Overview](#api-overview)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)

---

## Architecture Overview

```
┌──────────┐      ┌──────────────┐      ┌──────────┐
│  Client   │─────▶│  Spring Boot  │─────▶│ PostgreSQL│
│ (REST API)│◀─────│   Application │◀─────│    15     │
└──────────┘      └──────┬───────┘      └──────────┘
                         │ publish / consume
                  ┌──────▼───────┐
                  │   RabbitMQ    │
                  │  (Topic Exch.)│
                  └──────────────┘
```

The application follows **Hexagonal Architecture** (Ports & Adapters), with each business module cleanly separated into:

- **Domain** — entities, enums, business rules, exceptions
- **Application** — use cases, ports (interfaces), DTOs
- **Infrastructure** — controllers, persistence, messaging adapters

Modules communicate asynchronously through RabbitMQ events, keeping them decoupled and independently evolvable.

---

## Tech Stack

| Layer          | Technology                                       |
|----------------|--------------------------------------------------|
| Language       | Java 17                                          |
| Framework      | Spring Boot 3.5                                  |
| Database       | PostgreSQL 15 + Flyway migrations (16 versions)  |
| Messaging      | RabbitMQ 3 (topic exchange, 4 queues)            |
| Security       | Spring Security + JWT (JJWT 0.12)               |
| Scheduling     | ShedLock (distributed lock for timeouts)         |
| API Docs       | OpenAPI 3.0 (springdoc / Swagger UI)             |
| Containers     | Docker + Docker Compose (multi-stage build)      |
| Testing        | JUnit 5, Mockito, Testcontainers, spring-rabbit-test |
| CI             | GitHub Actions                                   |

---

## Domain Modules

### Identity
User registration with admin approval workflow. Supports role management (`USER`, `ANALYST`, `ADMIN`) and user status transitions (`PENDING` → `ACTIVE` / `REJECTED` / `SUSPENDED` / `BLOCKED`). Includes JWT authentication, PIN-based transaction confirmation with brute-force lockout, and Brazilian document (CPF/CNPJ) validation.

### Banking
Transfer creation, balance reservation, fund settlement/reversal, and account management. When a user is approved, an account is automatically created via event. Transactions follow a strict state machine with validated transitions.

### Risk
Pluggable risk scoring engine that evaluates transactions using behavioral rules. Consumes `TransactionCreatedEvent`, scores the transaction, and publishes `TransactionAnalyzedEvent` back to the banking module. Designed as a drop-in slot for a future ML model.

### Analyst
Review queue for medium/high-risk transactions awaiting human decision. Analysts can approve or deny flagged transactions with a mandatory reason. Provides filtered views with full risk analysis and decision history.

### Admin
Dashboard with operational metrics, user moderation tools (approve, suspend, block, role changes), transaction oversight with filters (status, risk level, date range), and configurable risk thresholds.

---

## Event-Driven Pipeline

All inter-module communication flows through a **RabbitMQ topic exchange** (`riskguard.events`):

```
User Approved                    Transfer Created
     │                                │
     ▼                                ▼
 ┌────────────┐  user.approved   ┌────────────┐  transaction.created   ┌────────────┐
 │  Identity   │────────────────▶│  Banking    │──────────────────────▶│    Risk     │
 │   Module    │                 │   Module    │◀──────────────────────│   Module    │
 └────────────┘                  └─────┬──────┘  transaction.analyzed  └────────────┘
                                       │
                                       │ transaction.status.*
                                       ▼
                                ┌──────────────┐
                                │ Notification  │
                                │    Queue      │
                                └──────────────┘
```

**Queues:**
| Queue | Consumer | Purpose |
|---|---|---|
| `riskguard.risk.transaction-created` | Risk module | Triggers risk analysis |
| `riskguard.banking.transaction-analyzed` | Banking module | Applies risk result to transaction |
| `riskguard.banking.user-approved` | Banking module | Creates account on user approval |
| `riskguard.notification.transaction-status` | External consumers | Status change notifications |

Events are published **after commit** using `TransactionSynchronization` to prevent lost events on rollback.

---

## Risk Scoring Engine

The scoring engine uses a **composite strategy pattern** — each rule independently contributes a risk signal, and the final score is the sum (capped at 1.0).

### Rules

| Rule | What it detects | Max contribution |
|---|---|---|
| **Amount Deviation** | Transfer amount vs. sender's 30-day average | 0.40 |
| **Transaction Frequency** | Unusual burst of transactions (1h / 24h windows) | 0.25 |
| **New Receiver** | First-ever transaction to this receiver | variable |
| **Off Hours** | Transactions outside business hours | variable |
| **Receiver Profile** | Receiver has fraud history or suspect flag | variable |

### Classification

The final score maps to a risk level using admin-configurable thresholds:

| Score Range | Risk Level | Flow |
|---|---|---|
| 0.00 – 0.39 | `LOW` | Auto-approved, funds settled |
| 0.40 – 0.69 | `MEDIUM` | Customer must confirm (PIN), timeout applies |
| 0.70 – 1.00 | `HIGH` | Routed to analyst queue for manual review |

The engine outputs a human-readable explanation with each signal's contribution, so analysts see exactly why a transaction was flagged.

### Extensibility

The `RiskScoringStrategy` interface makes it straightforward to swap the rule engine for an ML model — the pipeline, events, and classification logic remain unchanged.

```
RiskScoringStrategy (interface)
       │
       ├── CompositeRiskScoringStrategy (current: rule-based)
       └── [Future: MLScoringStrategy]
```

---

## Transaction Lifecycle

```
CREATED ──▶ ANALYZING ──┬──▶ APPROVED (low risk) ──▶ Funds settled
                        │
                        ├──▶ AWAITING_CUSTOMER (medium risk)
                        │       ├── Customer confirms (PIN) ──▶ APPROVED
                        │       ├── Customer disputes ──▶ DENIED ──▶ Funds reverted
                        │       └── Timeout (scheduled) ──▶ AWAITING ANALYST ──▶ Analyst decides
                        │
                        └──▶ AWAITING_ANALYST (high risk)
                                ├── Analyst approves ──▶ APPROVED
                                └── Analyst denies ──▶ DENIED ──▶ Funds reverted
                                └── Analyst reports fraud ──▶ FRAUD CONFIRMED ──▶ Funds reverted 

APPROVED ──▶ DISPUTED (customer opens post-approval dispute)
         ──▶ FRAUD_CONFIRMED (investigation result)
```

Funds are **reserved** on transaction creation and only **settled** or **reverted** based on the final outcome. A distributed scheduler (ShedLock) handles customer confirmation timeouts.

---

## API Overview

The full API is documented via **Swagger UI** at `/swagger-ui.html` when the application is running.

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register new user (CPF/CNPJ validated) |
| POST | `/auth/login` | Authenticate, receive JWT token |

### User (requires `USER` role)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/user/me` | Current user profile |
| GET | `/user/transfers` | Transfer history (paginated) |
| POST | `/user/pin` | Set transaction PIN |

### Transfers (requires `USER` role)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/transfers` | Create transfer |
| GET | `/transfers/{id}/status` | Check transaction status |
| POST | `/transfers/{id}/customer-confirmation` | Confirm or dispute (requires PIN) |
| POST | `/transfers/{id}/dispute` | Open dispute on approved transfer |

### Analyst (requires `ANALYST` role)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/analyst/transactions` | Review queue (filterable) |
| GET | `/analyst/transactions/{id}` | Transaction detail with risk breakdown |
| POST | `/transfers/{id}/analyst-decision` | Approve or deny with reason |

### Admin (requires `ADMIN` role)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/stats` | Dashboard metrics |
| GET | `/admin/transactions` | All transactions (filterable) |
| GET | `/admin/users` | All users (filterable) |
| POST | `/admin/users/{id}/approve` | Approve pending user |
| POST | `/admin/users/{id}/suspend` | Suspend user |
| POST | `/admin/users/{id}/block` | Block user |
| PATCH | `/admin/users/{id}/role` | Change user role |

*Plus additional endpoints for deny, unsuspend, unblock, PIN reset, and decision history.*

---

## Getting Started

### Prerequisites

- **Java 17**
- **Maven 3.9+**
- **Docker** and **Docker Compose**

### 1. Clone the repository

```bash
git clone https://github.com/oantoniovinicius/risk-guard.git
cd risk-guard
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` with your credentials:

```env
POSTGRES_DB=riskguard
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-password

RABBITMQ_DEFAULT_USER=your-user
RABBITMQ_DEFAULT_PASS=your-password

APP_JWT_SECRET_BASE64=your-base64-encoded-secret
```

### 3. Start the infrastructure

```bash
docker compose up -d
```

This starts **PostgreSQL** (port 5433), **RabbitMQ** (ports 5672 / 15672), and the **application** (port 8081).

### 4. Access the application

- **API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672

---

## Running Tests

Tests use **Testcontainers** to spin up isolated PostgreSQL and RabbitMQ instances — no external services needed.

```bash
mvn test
```

### Test coverage

- **Unit tests** — individual risk rules, use cases, domain logic
- **Integration tests** — full request lifecycle through controllers, messaging pipelines, event-driven flows
- **Testcontainers** — real PostgreSQL and RabbitMQ containers for integration tests, matching production behavior

CI runs automatically on every push and pull request to `main` via GitHub Actions.

---

## Project Structure

```
src/main/java/com/galeritos/risk_guard/
│
├── identity/                  # User registration, auth, PIN management
│   ├── domain/                #   Entities, enums, exceptions
│   ├── application/           #   Use cases, ports, DTOs
│   └── infrastructure/        #   Controllers, persistence, JWT, messaging
│
├── banking/                   # Transfers, accounts, fund management
│   ├── domain/                #   Transaction state machine, account model
│   ├── application/           #   Use cases, event publishers
│   └── infrastructure/        #   Controllers, persistence, messaging, scheduling
│
├── risk/                      # Risk analysis engine
│   ├── domain/                #   RiskAnalysis model, scoring strategy interface
│   ├── application/           #   Scoring rules, context loader, use cases
│   └── infrastructure/        #   Messaging listeners, persistence
│
├── analyst/                   # Analyst review queue and transaction detail
│   ├── application/           #   Use cases for listing and viewing
│   └── infrastructure/        #   Controllers, mappers
│
├── admin/                     # Dashboard, user moderation, settings
│   ├── domain/                #   AdminSettings, decision history
│   ├── application/           #   Use cases for moderation and stats
│   └── infrastructure/        #   Controllers, persistence, mappers
│
├── shared/                    # Cross-cutting: enums, events, validation, exceptions
└── config/                    # Security, RabbitMQ, OpenAPI, scheduling config

src/main/resources/
├── application.yaml           # Application configuration
└── db/migration/              # Flyway migrations (V1–V16)

src/test/java/                 # Unit + integration tests (49 test classes)
```

---

## License

This project was built for learning purposes.
