# TradeX - Distributed Stock Trading Platform

A distributed, high-performance stock exchange and trading platform with authentic distributed systems capabilities, deterministic paper-trading order execution, and fault tolerance.

---

## Architecture Overview

- **Frontend**: React 18, TypeScript, Vite, Modern Dashboard UI
- **API Gateway**: Spring Boot 3, REST API, Portfolio & Wallet Ledger, Failure Detector & Failover Coordinator
- **Distributed Nodes (`node1`, `node2`, `node3`)**: Multi-threaded matching engine, Java RMI coordination, Lamport & Vector logical clocks, Bully leader election, Primary-Backup replication.
- **Durable Database**: PostgreSQL for persistent orders, trades, wallet ledger, and audit history.

---

## Phase Implementations

### Phase 1–3: Core Foundation & Market Data
- Multi-asset order books and real-time market data streaming.
- User management and double-entry wallet accounting.

### Phase 4–6: Trading Engine, Portfolio & Auditing
- Deterministic matching engine (Price-Time Priority) with Market, Limit, and Stop-Loss orders.
- Weighted-average cost basis portfolio calculation and real-time P&L analytics.
- Immutable audit log and event notifications.

### Phase 7: Inter-Node RMI & Multi-threading
- Shared Java RMI contracts (`RemoteNodeService`) with bounded worker pools.
- Dedicated thread pools for non-blocking order intake and execution.

### Phase 8: Distributed Logical Clocks
- Lamport logical clock synchronization across inter-node RPCs.
- 3-node Vector clock state capturing causal relationships and concurrency detection.

### Phase 9: Primary-Backup Replication & Consistency
- Authoritative primary matching engine on cluster leader.
- Monotonically increasing committed sequence numbers per epoch.
- Replica acknowledgement, lag tracking, and idempotent event application.

### Phase 10: Fault Detection, Failover & Recovery
- **Periodic Heartbeats & Failure Detection**:
  - Heartbeat monitor pinging all nodes every 2500ms.
  - Three-tier status categorization: `HEALTHY` (reachable), `SUSPECTED` (1–2 missed beats), `CONFIRMED_UNAVAILABLE` (≥3 missed beats).
- **Controlled Server-Side Fault Injection**:
  - Endpoints `/api/fault/crash` and `/api/fault/recover` on distributed nodes.
  - Causes genuine `RemoteException` within RMI stubs and HTTP 503 on health checks, simulating true network partitions and node crashes.
- **Automatic Failover & Split-Brain Safeguards**:
  - Unavailability of current leader initiates Bully leader election on the highest-priority surviving node.
  - Increments monotonic **Leader Epoch** to fence stale leader writes.
  - Rebuilds authoritative matching state from durable PostgreSQL records.
- **Trading Safety & Safe Write Suspension**:
  - Trading status shifts to `SUSPENDED` during failover, rejecting or holding authoritative writes to prevent duplicate executions or inconsistent balances.
  - Resumes `AVAILABLE` only after successful leadership confirmation and state catch-up.
- **Interactive Web Dashboard**:
  - Dedicated route `/failover` displaying cluster heartbeat matrix, reachability, latency, active leader epoch, trading state badge (`AVAILABLE`, `DEGRADED`, `SUSPENDED`), and controls to crash or recover any node in real-time.

### Phase 11: Bully & Ring Leader Elections
- **Bully Algorithm Integration**:
  - Initiates with configured node priorities (`node1=1, node2=2, node3=3`).
  - Sends `ELECTION` to higher-priority peers, handles `OK` responses and timeouts, announces `COORDINATOR` with monotonically incremented **Leader Epoch**.
- **Ring Algorithm Integration**:
  - Circulates election token around logical ring (`node1 -> node2 -> node3 -> node1`), collects candidate priorities, bypasses unavailable nodes dynamically, and announces coordinator across the circuit.
- **Shared Authoritative Leader Management**:
  - Both algorithms update the unified cluster leader registry (`currentActiveLeader` & `currentLeaderEpoch`).
  - Epoch fencing prevents stale election messages and out-of-date coordinators from overwriting active leadership.
  - Interactive live order routing probe verifies immediate failover to newly elected matching engine leader.
- **Frontend Election & Topology Console**:
  - View real-time message exchanges, participating nodes, unavailable bypasses, elected coordinator, duration, and leader epoch.
  - Interactive controls to run either Bully or Ring from any node, simulate crashes, and test authoritative order routing.

### Phase 12: Distributed Load Balancing
- **Load Balancing Algorithms**:
  - Baseline **Round Robin** sequencing across healthy cluster nodes.
  - Health-aware **Least Connections** routing requests to nodes with lowest active concurrent requests.
- **Category-Based Routing Policy**:
  - Read-only market data (`/api/market/stocks`, `/api/market/prices`) and analytics are dynamically load balanced across healthy replicas.
  - Critical state writes, order execution, and matching engine operations are strictly routed to the authoritative cluster leader.
  - Unhealthy or crashed nodes are automatically excluded from the candidate pool.
- **Real-Time Load Balancing Dashboard**:
  - Route `/load-balancer` visualizes active algorithm selector, real per-node request counters, concurrent active requests, average latency, and error rate percentages.
  - Safe non-financial load test generator with configurable probe counts.
  - Live routing decision audit log detailing category, selected target node, strategy, and selection reasoning.

---

## Service Endpoints & Ports

| Service | Port | Description |
| :--- | :--- | :--- |
| **Frontend** | `3000` | Web Trading Dashboard |
| **Gateway** | `8080` | Public API Gateway & Failover Coordinator |
| **Node 1** | `8081` (HTTP) / `1099` (RMI) | Distributed Node 1 (Priority 1) |
| **Node 2** | `8082` (HTTP) / `1099` (RMI) | Distributed Node 2 (Priority 2) |
| **Node 3** | `8083` (HTTP) / `1099` (RMI) | Distributed Node 3 (Default Leader, Priority 3) |
| **PostgreSQL** | `5432` | Durable Relational Store |

---

## Running with Docker Compose

1. Copy environment configuration:
   ```bash
   cp .env.example .env
   ```
2. Build and start all distributed containers:
   ```bash
   docker-compose up --build
   ```
3. Open your browser to `http://localhost:3000`.

---

## Phase 13: Analytics, Administration and Observability

### 1. Trading Analytics Service
- **Persisted Volume & Trade Aggregations**: Queries durable records (`TradeRepository`, `OrderRepository`, and `StockRepository`) to compute executed trade volume, total fill values, and trade counts without synthetic inflation.
- **Order Flow & Execution Metrics**: Computes exact fill rates, cancellation rates, and rejection rates, distinguishing buy volume vs. sell volume ratios.
- **Active Instruments & Market Movers**: Aggregates per-instrument volume and identifies top gainers/losers by evaluating active prices against initial market baseline prices.

### 2. Administrative Console
- **Account & System Governance**: Role-protected views listing registered users, account lock statuses, balances, and system orders.
- **Audited Emergency Circuit Breaker**: Allows administrators to pause and resume all exchange order routing dynamically, emitting durable audit events and notifying connected clients in real-time.

### 3. Unified Distributed-Systems Control Center
- **Consolidated Dashboard**: A single control view (`/control-center`) integrating tabs for:
  - **Cluster Topology & Node Health**
  - **Logical (Lamport & Vector) and Physical Clocks**
  - **Bully & Ring Elections**
  - **Primary-Backup Replication Lag & Ack History**
  - **Fault Injection & Failover Recovery**
  - **Distributed Load Balancing & Strategy Switching**

### 4. Real-time Telemetry & WebSockets
- **Cluster Events Stream (`/ws/cluster`)**: Broadcasts node health transitions, leader re-elections, replication pulses, and circuit-breaker triggers.
- **Resilient Frontend Handlers**: WebSocket connections handle reconnections, authentication tokens, and graceful degradation during network partitions.

