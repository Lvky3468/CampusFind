# CampusFind — Campus Lost & Found Portal

CampusFind is a Java 17 / Spring Boot / MySQL web application with a responsive HTML, CSS, and JavaScript interface. The Spring Boot application serves the website from `backend/src/main/resources/static` and exposes JSON APIs under `/api`.

## Requirements

- Java 17+
- Maven 3.8+
- MySQL 8+

## Configure and run

1. Create a database: `CREATE DATABASE lostfound;`
2. Set the following environment variables before starting the backend:
   - `DB_URL` — JDBC URL (defaults to local MySQL database `lostfound`)
   - `DB_USERNAME` — MySQL user (defaults to `root`)
   - `DB_PASSWORD` — MySQL password (required)
   - `JWT_SECRET` — random secret of at least 32 bytes (required)
   - `JWT_EXPIRATION_MS` — token lifetime in milliseconds (optional, defaults to 86400000)
   - `PORT` — HTTP port (optional, defaults to 8080)
3. Run from the `backend` directory:

   ```bash
   mvn clean test
   mvn spring-boot:run
   ```

4. Open `http://localhost:8080`.

On Windows PowerShell, set environment variables for the current terminal like this (replace the values):

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/lostfound?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "YOUR_LOCAL_MYSQL_PASSWORD"
$env:JWT_SECRET = "GENERATE_A_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG"
cd backend
mvn clean test
mvn spring-boot:run
```

## Main APIs

- `POST /api/auth/register` — register a user (`username`, `email`, `password`, optional `fullName` JSON fields)
- `POST /api/auth/login` — sign in; returns a bearer token
- `GET /api/items` — browse items; supports `q`, `location`, `category`, `isLost`, `page`, and `size`
- `GET /api/items/stats` — live database counts for total, lost, found, and returned items
- `GET /api/items/{id}` — item details
- `GET /api/items/photo/{id}` — item photo
- `POST /api/items` — multipart report creation; lost reports require login
- `POST /api/claims/{itemId}` — submit a claim (authenticated)
- `GET /api/claims/my` — current user's claims
- `GET /api/claims/pending` and claim resolution endpoints — administrator only

Send the JWT using `Authorization: Bearer <token>` for protected APIs.

## Security and deployment notes

- Database credentials and JWT signing secrets are read from environment variables, not source code.
- `User.password` and item contact phone numbers are excluded from JSON serialization.
- Public metrics come from the database, not hardcoded sample values.
- Use HTTPS in production, configure a strong unique `JWT_SECRET` in your deployment secret manager, restrict database network access, and back up the database.
- `spring.jpa.hibernate.ddl-auto=update` is retained for compatibility with this project. For a production deployment, use reviewed migrations and set an explicit schema-management policy.
- If any credentials previously committed or shared with this project were real, rotate them immediately (including database passwords and JWT secrets). Removing them from the current files does not invalidate copies in Git history or old deployments.

## Verification status

The code changes in this package should be validated with `mvn clean test` and an integration run against a configured MySQL database. A successful build alone does not verify database access, browser rendering, or all user journeys. Do not consider the deployment production-ready until those checks pass.
