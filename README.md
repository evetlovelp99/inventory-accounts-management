# BeeWax ERP

Inventory and accounts receivable/payable system (React + Spring Boot + MySQL).

## Local setup

1. Copy `.env.example` to `.env` and fill in required values (at minimum `DB_PASSWORD` and `JWT_SECRET` for local backend).
2. Export the variables from `.env` into your shell (or configure them in your IDE run configuration).
3. Start MySQL and ensure the database named in `DB_NAME` exists.
4. Backend: `cd backend && ./mvnw spring-boot:run`
5. Frontend: `cd frontend && npm install && npm run dev`
6. Open http://localhost:5173
