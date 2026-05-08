# Basketball Manager API Documentation

## Common Requirements for All Secured Endpoints

- **JWT Bearer Token** — Must be included in every request (except `/register` and `/login`) via the `Authorization: Bearer <token>` header.
- **UUID Path Variables** — `scenarioId`, `teamId`, `playerId`, `gameId` must be valid UUIDs belonging to the authenticated user's scenario.
- **WebSocket** — Live games (`/start`, `/watch`) push events to `/topic/game/{gameId}` using STOMP over WebSocket. The client must subscribe to that topic to receive play-by-play `GameEvent` messages.

---

## 🔐 Auth API — `/api/auth`

| Method | Endpoint | Description | Required |
|--------|----------|-------------|----------|
| `POST` | `/api/auth/register` | Register a new account | `RegisterRequest` body (validated): username, password, etc. |
| `POST` | `/api/auth/login` | Login and receive a JWT token | `LoginRequest` body (validated): username, password |
| `POST` | `/api/auth/logout` | Logout (stateless — client discards token) | Valid JWT in `Authorization` header |
| `GET` | `/api/auth/me` | Get info about the authenticated user | Valid JWT in `Authorization` header |

---

## 🏀 Scenario API — `/api/scenarios`

| Method | Endpoint | Description | Required |
|--------|----------|-------------|----------|
| `POST` | `/api/scenarios` | Create a new scenario | JWT + `CreateScenarioRequest` body (validated) |
| `GET` | `/api/scenarios` | Get all scenarios for the authenticated user | JWT |
| `GET` | `/api/scenarios/{scenarioId}` | Get a specific scenario by ID | JWT + `scenarioId` (UUID) |
| `DELETE` | `/api/scenarios/{scenarioId}` | Delete a specific scenario | JWT + `scenarioId` (UUID) |

---

## 👥 Team API — `/api/scenarios/{scenarioId}/teams`

| Method | Endpoint | Description | Required |
|--------|----------|-------------|----------|
| `GET` | `/api/scenarios/{scenarioId}/teams` | Get all teams in a scenario | JWT + `scenarioId` (UUID) |
| `GET` | `/api/scenarios/{scenarioId}/teams/{teamId}/roster` | Get the full roster of a specific team | JWT + `scenarioId` (UUID) + `teamId` (UUID) |

---

## 🧑 Player API — `/api/scenarios/{scenarioId}`

| Method | Endpoint | Description | Required |
|--------|----------|-------------|----------|
| `GET` | `/api/scenarios/{scenarioId}/free-agents` | Get all free agents (unrostered players) in a scenario | JWT + `scenarioId` (UUID) |
| `GET` | `/api/scenarios/{scenarioId}/players/{playerId}` | Get detailed info for a specific player | JWT + `scenarioId` + `playerId` (UUID) |
| `GET` | `/api/scenarios/{scenarioId}/players/{playerId}/stats` | Get season stats for a specific player | JWT + `scenarioId` + `playerId` (UUID) |

---

## 🎮 Game API — `/api/scenarios/{scenarioId}/games`

| Method | Endpoint | Description | Required |
|--------|----------|-------------|----------|
| `POST` | `/api/scenarios/{scenarioId}/games/start` | Start a live WebSocket game between two teams | JWT + `scenarioId` + body: `{ "homeTeamId": UUID, "awayTeamId": UUID }` |
| `POST` | `/api/scenarios/{scenarioId}/games/season/{yearNumber}/generate` | Generate a full round-robin season schedule | JWT + `scenarioId` + `yearNumber` (int) |
| `GET` | `/api/scenarios/{scenarioId}/games/season/{yearNumber}` | Retrieve the schedule for a specific season year | JWT + `scenarioId` + `yearNumber` (int) |
| `GET` | `/api/scenarios/{scenarioId}/games/season/{yearNumber}/standings` | Get current standings for a season year | JWT + `scenarioId` + `yearNumber` (int) |
| `POST` | `/api/scenarios/{scenarioId}/games/{gameId}/simulate` | Instantly simulate a scheduled game (no WebSocket) | JWT + `scenarioId` + `gameId` (UUID) |
| `POST` | `/api/scenarios/{scenarioId}/games/{gameId}/watch` | Start live WebSocket streaming of a scheduled game | JWT + `scenarioId` + `gameId` (UUID) → subscribe to `/topic/game/{gameId}` |
| `POST` | `/api/scenarios/{scenarioId}/games/{gameId}/skip` | Fast-forward a currently-running watched game to its end | JWT + `scenarioId` + `gameId` (UUID) |

