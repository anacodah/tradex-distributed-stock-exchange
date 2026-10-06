# TradeX - Distributed Stock Trading Platform

This is the initial foundation phase for TradeX.

## Architecture

- **Frontend**: React, TypeScript, Vite
- **Gateway**: Spring Boot (API Gateway)
- **Distributed Nodes (3)**: Spring Boot instances of the same application configured via environment variables
- **Database**: PostgreSQL

## Ports
- Frontend: 3000
- Gateway: 8080
- Node 1: 8081
- Node 2: 8082
- Node 3: 8083
- Database: 5432

## Running the Application

1. Create a `.env` file from `.env.example`
2. Run Docker Compose:
   ```bash
   docker-compose up --build
   ```
3. Access the frontend at `http://localhost:3000`
