# Bridge Control - BD Bank Compliance Backend

## Overview

Bridge Control is an internal compliance monitoring tool designed to verify business rules on banking data without exposing the database to the frontend. This Java Spring Boot backend translates business rules written in a Domain Specific Language (DSL) into SQL queries, executes them in read-only mode on the bank database (bd_bank), and generates alerts when anomalies are detected.

The project exposes a REST API for managing rules, alerts, schema, scope, and analysis frequency. API documentation is available via Swagger at `http://localhost:8080/swagger-ui.html` after backend startup.

## Key Features

- **DSL Rule Engine**: Write business rules in a simple domain-specific language that translates to SQL
- **Read-Only Database Access**: Secure connection to banking databases with SELECT-only permissions
- **Real-Time Alert Generation**: Automatic detection and reporting of compliance violations
- **Schema Introspection**: Automatic discovery of database tables and columns
- **Logical Renaming**: Customize table and column names for better readability in the interface
- **Scheduled Execution**: Configurable frequency for rule execution (interval or cron expressions)
- **Authentication & Authorization**: Secure admin access with session management
- **Multi-Database Support**: Compatible with MySQL and PostgreSQL

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Node.js 18+ (for frontend development)

## Local Setup

### Environment Configuration

1. Copy `.env.example` to `.env` and adjust values if needed:

   ```bash
   cp .env.example .env
   ```

   **Note for Windows (PowerShell)**: Wherever you see `set -a; source .env; set +a` (bash syntax), use `.\scripts\load-env.ps1` instead. Other commands (`docker`, `mvn`, `git`) remain the same.

### Database Setup

2. Start the local test database (MySQL with initial dataset):

   ```bash
   docker compose up -d
   ```

### Backend Startup

3. Load environment variables and start the application:

   ```bash
   # Linux/Mac
   set -a; source .env; set +a
   mvn spring-boot:run

   # Windows PowerShell
   .\scripts\load-env.ps1
   mvn spring-boot:run
   ```

### Verification

4. Verify compilation and Spring context startup (validates database connection):

   ```bash
   mvn clean compile
   mvn test
   ```

**Security Note**: The MySQL account used (`DB_USER`/`DB_PASSWORD`) is a dedicated technical account for the application. It is restricted to read-only access (`SELECT`) on the test database and must not be used to modify data.

## Security Architecture

### Read-Only Database Access

The application account is strictly read-only with explicit `GRANT SELECT` permissions defined in `db/init/002_create_readonly_user.sh`. A generic `JdbcConnectionService` layer (MySQL/PostgreSQL compatible) validates the connection at startup.

To verify the read-only restrictions:

1. Reset the test database to ensure new init scripts run (they only execute on first startup of an empty volume):

   ```bash
   docker compose down -v
   docker compose up -d
   ```

2. Verify the account is restricted to SELECT:

   ```bash
   # Should work
   docker exec -it bd-bank-test-db mysql -u bdbank_readonly -p"change_me" bd_bank_test -e "SELECT * FROM clients;"

   # Should be denied (ERROR 1142 : INSERT command denied)
   docker exec -it bd-bank-test-db mysql -u bdbank_readonly -p"change_me" bd_bank_test -e "INSERT INTO clients (nom, email) VALUES ('test','test@test.com');"
   ```

3. Start the application and verify the log line `Connexion bd_bank OK -> MySQL ...`:

   ```bash
   set -a; source .env; set +a
   mvn spring-boot:run
   ```

4. Run tests (requires test database running):

   ```bash
   mvn test
   ```

## Project Structure

```
bd-bank-compliance-backend/
├── src/main/java/com/bridge/bdbank/
│   ├── api/              # REST controllers and DTOs
│   ├── auth/             # Authentication and session management
│   ├── config/           # Spring configuration (CORS, datasources)
│   ├── connection/       # JDBC connection service
│   ├── dsl/              # DSL parsing with ANTLR4
│   ├── execution/        # Rule execution and scheduling
│   ├── introspection/    # Database schema introspection
│   ├── mapping/          # Logical renaming of tables/columns
│   ├── persistence/      # JPA entities and repositories
│   ├── scope/            # Scope management
│   ├── translation/      # DSL to SQL translation
│   └── validation/       # Rule validation service
├── src/main/resources/
│   └── application.yml   # Spring Boot configuration
├── src/test/            # Unit and integration tests
├── db/init/             # Database initialization scripts
├── Frontend/             # React frontend application
└── pom.xml              # Maven configuration
```

## Git Workflow

Branches are organized by development milestones, named `jour-XX-slug` (e.g., `jour-01-init-projet`, `jour-02-connexion-jdbc`). Each branch is merged into `main` once the day's objective is validated.

## Frontend Setup

In a separate terminal, from the `Frontend` directory:

```powershell
cd Frontend
npm install
npm run dev
```

The frontend is available at `http://127.0.0.1:5173`. To use the real API, ensure `Frontend/.env` contains:
```
VITE_API_URL=http://localhost:8080
VITE_DEMO_MODE=false
```

In production mode, the dashboard reloads tables, scope, rules, alerts, and frequency from the API. The control posture is calculated from the active rule rate, scope coverage, and resolved alert rate. The refresh button forces a new data load.

From an alert detail view, the status button allows marking it as resolved or automatically reactivating it based on analysis cycles. To activate or deactivate a rule, use the corresponding action in the rules list; it updates the rule via `PUT /rules/{id}`.

## Administrator Setup

The project is designed for a single administrator. If no user exists yet, define a private key in `.env`:

```env
BDBANK_SETUP_KEY=a-long-and-secret-value
```

Restart Spring Boot, then open `http://127.0.0.1:5173/setup`. The form requires this key, a valid username, and a password of at least 12 characters. The `/auth/setup` endpoint is denied once an account exists and is not shown in navigation.

After initialization, log in at `http://127.0.0.1:5173/`. Protected routes use the Bearer token returned by `/auth/login`; sessions are invalidated on logout and expire after 15 minutes of inactivity.

## API Documentation

Once the backend is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

This provides interactive documentation for all REST endpoints including:
- Authentication (`/auth/*`)
- Rules management (`/rules/*`)
- Alerts management (`/alerts/*`)
- Schema introspection (`/schema/*`)
- Scope configuration (`/scope/*`)
- Frequency configuration (`/config/frequency`)
- Logical renaming (`/mappings/*`)

## DSL Rule Syntax

Business rules are written in a simple domain-specific language that supports:

- **Comparisons**: `column < value`, `column > value`, `column = value`, `column != value`
- **Logical operators**: `AND`, `OR`
- **Null checks**: `column IS NULL`, `column IS NOT NULL`
- **Aggregations**: `COUNT(column)`, `SUM(column)`, `AVG(column)`, `MAX(column)`, `MIN(column)`

Examples:
```
solde < 0 AND decouvert_autorise = false
montant > 10000
email IS NULL
COUNT(transactions) > 200
```

## Testing

Run the test suite with:
```bash
mvn test
```

Tests require the test database to be running via Docker Compose. The test suite includes:
- Unit tests for service layers
- Integration tests for controllers
- DSL parsing and translation tests
- Database connection tests

## License

Internal project - BD Bank Compliance Tool
