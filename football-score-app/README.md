# Football Score + WhatsApp Notification App

A personalized football monitoring system with reliable WhatsApp notifications for 5 competitions:
- Premier League
- La Liga
- Serie A
- UEFA Champions League
- Indian Super League (ISL)

## Architecture

```
Frontend (React + Vite + Tailwind)     Backend (Spring Boot 3 + Java 21)
        │                                        │
        │           HTTPS / REST API             │
        ▼                                        ▼
   Vercel (Static)                    Railway/Render (Spring Boot)
                                                        │
                        ┌─────────────────────────────┼─────────────────────────────┐
                        ▼                             ▼                             ▼
                  PostgreSQL                      Redis                          WhatsApp
                  (Primary)                    (Cache/Queue)               Business API
                        ▲                             ▲                             ▲
                        │                             │                             │
                  Sportmonks API                                                                
                  (Football Data)
```

## Features

- **Match Monitoring**: Backend continuously monitors matches even when frontend is closed
- **WhatsApp Notifications**: Match start reminders, goals, cards, substitutions, half-time, full-time
- **Idempotent Events**: Duplicate notifications prevented via deterministic event keys
- **Reliable Delivery**: Notification queue with exponential backoff retry (max 4 attempts)
- **Favorites**: Star matches and teams to receive notifications
- **Multi-timezone**: UTC storage, user timezone conversion at boundary

## Tech Stack

### Backend
- Java 21, Spring Boot 3.4
- PostgreSQL 16 (Flyway migrations)
- Redis 7 (Cache, Queue, Distributed Locks)
- Resilience4j (Retry, Circuit Breaker, Rate Limiter)
- ShedLock (Distributed Scheduling)
- JWT Authentication
- Sportmonks Football API

### Frontend
- React 18, Vite, TypeScript
- Tailwind CSS
- TanStack Query (React Query)
- React Router

### Deployment
- Frontend: Vercel
- Backend: Railway (recommended) or Render
- Database: Managed PostgreSQL
- Cache: Managed Redis

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- Docker (for local PostgreSQL/Redis)
- Sportmonks API account
- WhatsApp Business Platform account

### Local Development

1. **Clone and configure**
   ```bash
   cd football-score-app
   cp backend/src/main/resources/application-local.properties.example backend/src/main/resources/application-local.properties
   # Edit application-local.properties with your credentials
   ```

2. **Start infrastructure**
   ```bash
   docker-compose up -d
   ```

3. **Run backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. **Run frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080/api
   - Health: http://localhost:8080/actuator/health

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `SPORTMONKS_API_TOKEN` | Sportmonks API token | Yes |
| `WHATSAPP_ACCESS_TOKEN` | WhatsApp System User token | Yes |
| `WHATSAPP_PHONE_NUMBER_ID` | WhatsApp Business Phone Number ID | Yes |
| `WHATSAPP_WABA_ID` | WhatsApp Business Account ID | Yes |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Random string for webhook verification | Yes |
| `WHATSAPP_APP_SECRET` | Meta App Secret | Yes |
| `JWT_SECRET` | 256-bit secret for JWT signing | Yes |
| `DATABASE_URL` | PostgreSQL connection URL | Yes (prod) |
| `REDIS_URL` | Redis connection URL | Yes (prod) |
| `FRONTEND_URL` | Frontend origin for CORS | Yes |

## API Endpoints

### Authentication
- `POST /api/auth/send-otp` - Send OTP via WhatsApp
- `POST /api/auth/verify-otp` - Verify OTP, return JWT tokens
- `POST /api/auth/refresh` - Refresh access token
- `GET /api/auth/me` - Get current user profile
- `PUT /api/auth/me/opt-in` - Update WhatsApp opt-in
- `PUT /api/auth/me/timezone` - Update user timezone

### Competitions
- `GET /api/competitions` - List all competitions
- `GET /api/competitions/{id}` - Get competition details
- `GET /api/competitions/{id}/matches` - Get matches for competition

### Matches
- `GET /api/matches/live` - Live matches
- `GET /api/matches/upcoming` - Upcoming matches
- `GET /api/matches/finished` - Finished matches
- `GET /api/matches/{id}` - Match details with events
- `GET /api/matches/{id}/events` - Match events
- `POST /api/matches/{id}/favorite` - Favorite a match
- `DELETE /api/matches/{id}/favorite` - Unfavorite a match

### Favorites
- `GET /api/favorites/matches` - User's favorite matches
- `GET /api/favorites/teams` - User's favorite teams
- `POST /api/favorites/teams/{teamId}` - Favorite a team
- `DELETE /api/favorites/teams/{teamId}` - Unfavorite a team

### Notifications
- `GET /api/notifications` - Notification history
- `GET /api/notifications/preferences` - Get preferences
- `PUT /api/notifications/preferences` - Update preferences

### Webhooks
- `GET /api/webhook/whatsapp` - WhatsApp webhook verification
- `POST /api/webhook/whatsapp` - WhatsApp status updates

## WhatsApp Templates Required

Create these Utility templates in WhatsApp Manager:

1. **match_starting_soon** - Match starting reminder
2. **goal_notification** - Goal scored
3. **match_event** - Cards, substitutions, half-time
4. **full_time_result** - Match finished

Variables: `{{team_home}}`, `{{team_away}}`, `{{league}}`, `{{minutes}}`, `{{score_home}}`, `{{score_away}}`, `{{team_scoring}}`, `{{minute}}`, `{{event_icon}}`, `{{event_type}}`

## Development

### Running Tests
```bash
cd backend
./mvnw test
```

### Database Migrations
```bash
cd backend
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/football_score
```

### Code Quality
```bash
cd backend
./mvnw checkstyle:check spotbugs:check
```

## Deployment

### Production Checklist
- [ ] All secrets in platform secret manager (not in code)
- [ ] PostgreSQL and Redis provisioned
- [ ] WhatsApp Business Account verified
- [ ] Message templates approved
- [ ] Webhook URL configured (HTTPS)
- [ ] CORS origin set to production frontend
- [ ] HTTPS enforced
- [ ] Monitoring/alerting configured

### Railway Deployment
1. Create new project
2. Add PostgreSQL and Redis services
3. Deploy backend from GitHub
4. Configure environment variables
5. Add cron job for scheduler
6. Add worker service for notifications

### Vercel Deployment
1. Import frontend repository
2. Set `VITE_API_URL` to production backend URL
3. Deploy

## Project Structure

```
football-score-app/
├── backend/
│   ├── src/main/java/com/example/football/
│   │   ├── auth/           # Authentication & JWT
│   │   ├── competition/    # Competition, Team, Standing
│   │   ├── match/          # Match, Season, Sync Service
│   │   ├── event/          # Match Events
│   │   ├── favorite/       # Favorite Matches/Teams
│   │   ├── notification/   # Notifications, Queue, Worker
│   │   ├── footballprovider/ # Sportmonks Integration
│   │   ├── whatsapp/       # WhatsApp Business API
│   │   ├── scheduler/      # ShedLock, Data Init
│   │   ├── config/         # Config, Security, CORS
│   │   └── exception/      # Global Exception Handling
│   └── src/main/resources/db/migration/  # Flyway SQL
├── frontend/
│   ├── src/
│   │   ├── components/     # Layout, UI Components
│   │   ├── pages/          # Home, Competition, Match, Favorites, Settings, Login
│   │   ├── services/       # API Client
│   │   ├── context/        # Auth Context
│   │   ├── hooks/          # Custom Hooks
│   │   └── types/          # TypeScript Types
│   └── public/
└── docs/                   # Research & Architecture Docs
```

## License

MIT