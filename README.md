# TimeCoin

TimeCoin is a student service marketplace backed by a Spring Boot API, MySQL, and a React frontend. Users can register, view TimeCoin market data, buy TimeCoin, and review their recent purchase history.

## Current Stack

- Frontend: React, React Router, Jest
- Backend: Java 21, Spring Boot, Spring Data JPA, Spring Security
- Database: MySQL 8
- Local orchestration: Docker Compose

## App Layout

- [backend/README.md](backend/README.md)
- [frontend/README.md](frontend/README.md)
- [STYLE.md](STYLE.md)
- [ROLES.md](ROLES.md)

## Run With Docker

From the repository root:

```bash
docker compose down -v
docker compose up --build
```

Services:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

The database is initialized from [db/init.sql](db/init.sql) and seeded from [db/seed.sql](db/seed.sql).

## Run Tests

Backend:

```bash
cd backend
./gradlew test
```

Frontend:

```bash
cd frontend
npm test -- --watchAll=false
```

## Current Feature Snapshot

- Registration: available
- Login: UI exists, real backend login is not implemented yet
- Coin price and supply: available
- Coin purchase endpoint: available
- Transaction history page and API: available
- Marketplace listing CRUD and real auth: not finished yet
