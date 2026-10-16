# FinFlow

FinFlow is a personal finance and banking platform I built with a Spring Boot API and a React frontend. It covers the full money loop: accounts, deposits, withdrawals and transfers, then budgeting, bills, savings goals, analytics and reports on top of that real ledger data.

My goal with this project was to build something that behaves like a real fintech backend, not a demo. Money is handled with `BigDecimal`, transfers are idempotent, the ledger is the single source of truth, and every request is scoped to the authenticated user.

## Screenshots

> Screenshots coming soon.

| Dashboard | Transactions | Budgets |
| --- | --- | --- |
| _screenshot_ | _screenshot_ | _screenshot_ |

## Tech stack

**Backend**
- Java 21, Spring Boot 4
- Spring MVC, Spring Security with stateless JWT authentication
- Spring Data JPA (Hibernate) with PostgreSQL
- Flyway for database migrations (`ddl-auto=validate`)
- Spring Mail for email verification and monthly reports
- Tesseract OCR for receipt scanning
- JUnit 5 for tests

**Frontend**
- React 18 with Vite
- React Router, Axios
- Tailwind CSS
- Recharts for analytics charts

## Features

- **Authentication:** registration, login, JWT sessions, email verification, phone verification with OTP, and a transaction PIN.
- **Accounts and money movement:** multiple accounts, deposits, withdrawals and transfers. Transfers are idempotent through the `Idempotency-Key` header.
- **Transactions:** searchable history with categories and transaction details.
- **Budgets:** overall and per-category budgets. Spending is calculated on the server from the ledger, never sent by the client.
- **Bills:** bill reminders that repeat one-time, weekly, monthly, quarterly or yearly. They only remind; nothing is debited automatically.
- **Savings goals:** goal tracking with a contribution history and a goal planning assistant.
- **Financial health score:** an explainable 0–100 planning score built from savings rate, budget use, bill coverage and goal progress.
- **Smart insights:** spending warnings, category suggestions and monthly summaries, with rule-based fallbacks when the narration provider isn't configured.
- **Notifications:** alerts for budget thresholds, due bills, low balances and unusual spending.
- **Reports:** a monthly financial report with CSV export, plus scheduled report emails.
- **Wallet and payments:** a wallet backed by your accounts, money reservations, deposit allocation rules, spending limits, and internal QR payments.
- **Cards:** card management that stores tokens only; raw card numbers and CVVs are never stored.
- **Family controls:** profiles managed by a parent, with allowances, category limits and freeze/unfreeze.
- **Receipts:** receipt upload and OCR scanning.
- **Investments:** investment education, a market snapshot, market news and a personal watchlist.
- **Security:** rate limiting on sensitive endpoints, a security audit log, transaction risk checks and signed webhooks.

## Project structure

```
finflow/
├── src/main/java/com/finflow/   # Spring Boot backend
├── src/main/resources/          # application config and Flyway migrations
├── src/test/                    # backend tests
└── frontend/                    # React + Vite client
```

## Getting started

### Prerequisites

- Java 21
- Node.js 18+ and npm
- PostgreSQL
- Tesseract (optional, only needed for receipt scanning)

### 1. Create the databases

```sh
createdb finflow
createdb finflow_test   # optional, for integration tests
```

### 2. Set environment variables

All configuration comes from environment variables. No secrets are stored in the repo.

**Required**

- `FINFLOW_DB_URL`
- `FINFLOW_DB_USERNAME`
- `FINFLOW_DB_PASSWORD`
- `FINFLOW_JWT_SECRET` (Base64-encoded, at least 256 bits)

**Optional**

| Area | Variables |
| --- | --- |
| Auth and admin | `FINFLOW_JWT_EXPIRATION_MS`, `FINFLOW_ADMIN_EMAILS` |
| Webhooks | `FINFLOW_WEBHOOK_SECRET`, `FINFLOW_WEBHOOK_PROVIDERS`, `FINFLOW_WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS` |
| Rate limits | `FINFLOW_RATE_LIMIT_LOGIN`, `FINFLOW_RATE_LIMIT_REGISTER`, `FINFLOW_RATE_LIMIT_PIN`, `FINFLOW_RATE_LIMIT_OTP`, `FINFLOW_RATE_LIMIT_QR`, `FINFLOW_RATE_LIMIT_WINDOW_SECONDS` |
| Limits | `FINFLOW_MONEY_MAXIMUM` |
| Email | `MAIL_HOST`, `MAIL_PORT`, `MAIL_STARTTLS`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `APP_BASE_URL` |
| Monthly reports | `FINFLOW_MONTHLY_REPORT_ENABLED`, `FINFLOW_MONTHLY_REPORT_CRON`, `FINFLOW_MONTHLY_REPORT_ZONE` |
| Phone verification | `PHONE_VERIFICATION_CHANNEL`, `PHONE_OTP_TTL_MINUTES`, `PHONE_OTP_MAX_ATTEMPTS` |
| News and markets | `NEWS_PROVIDER`, `NEWS_API_KEY`, `NEWS_CACHE_MINUTES`, `MARKET_DATA_PROVIDER`, `MARKET_PRICE_PROVIDER`, `MARKET_API_KEY` |
| Insight narration | `INSIGHT_NARRATION_PROVIDER`, `INSIGHT_NARRATION_TIMEOUT_MS`, `INSIGHT_NARRATION_MAX_TOKENS`, `HUGGINGFACE_API_URL`, `HUGGINGFACE_API_TOKEN`, `HUGGINGFACE_MODEL` |
| Receipt OCR | `RECEIPT_SCAN_PROVIDER`, `RECEIPT_SCAN_MAX_BYTES`, `RECEIPT_SCAN_MAX_UPLOAD`, `RECEIPT_TESSERACT_DATA_PATH`, `RECEIPT_TESSERACT_LIB_PATH`, `RECEIPT_TESSERACT_LANGUAGE` |
| Integration tests | `FINFLOW_TEST_DB_URL`, `FINFLOW_TEST_DB_USERNAME`, `FINFLOW_TEST_DB_PASSWORD` |
| Frontend | `VITE_API_BASE_URL` (set in `frontend/.env`) |

### 3. Run the backend

```sh
./mvnw spring-boot:run
```

The API runs on port `8080`. Health check: `GET /actuator/health`.

Flyway applies all migrations when the app starts. Hibernate runs in `validate` mode, so if the schema has drifted, startup fails instead of the database being changed silently.

### 4. Run the frontend

```sh
cd frontend
npm install
npm run dev
```

The Vite dev server runs on port `5173`. The backend CORS configuration has to allow that origin.

## Testing

```sh
./mvnw clean verify
```

Integration tests that need PostgreSQL are skipped unless the three `FINFLOW_TEST_DB_*` variables are set. Always point them at a separate test database.

To check that the frontend builds:

```sh
cd frontend
npm run build
```

## API overview

All routes except register and login need a valid JWT (`Authorization: Bearer <token>`).

| Area | Base routes |
| --- | --- |
| Auth and profile | `/api/auth/*`, `/api/users/me` |
| Accounts and transactions | `/api/accounts/*` |
| Analytics | `/api/analytics/summary`, `/api/analytics/categories`, `/api/analytics/monthly` |
| Planning | `/api/budgets`, `/api/bills`, `/api/goals`, `/api/financial-health` |
| Notifications and reports | `/api/notifications`, `/api/reports` |
| Wallet and payments | `/api/wallet`, `/api/money/*`, `/api/money-rules`, `/api/spending-limits`, `/api/payments/*` |
| Family | `/api/family/*` |
| Autopay | `/api/autopay` |
| Investments | `/api/investments/*` |

## Notes

- The financial health score is a planning indicator. It is not a credit score or financial advice.
- Autopay and market data are built behind provider interfaces. Until a real provider is configured, those endpoints report that the provider is unavailable instead of returning fake data.

## Author

**Harshit Kumar Gupta**, [@harshitkumargupta](https://github.com/harshitkumargupta)
