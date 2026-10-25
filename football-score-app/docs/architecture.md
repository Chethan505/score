# Architecture Decision Record

## Project: Football Score + WhatsApp Notification App

## Date: 2026-10-25

## Status: Accepted

---

## 1. Football Data Provider: Sportmonks

### Decision
Use **Sportmonks v3 Football API** as the primary data provider.

### Rationale
- Covers all 5 required competitions (Premier League, La Liga, Serie A, UCL, ISL)
- Consistent stats coverage across all leagues (critical for event detection)
- 99.99% uptime SLA with published metrics
- Official Java SDK for native Spring Boot integration
- Webhook support for push-based updates (reduces polling)
- Human support (7-day) with personal customer success contact
- Transparent per-league pricing: €29/mo for exactly 5 leagues
- Includes system enables single-request data fetching

### Alternatives Considered
- **API-Football**: Cheaper entry ($19/mo) but daily caps, inconsistent stats, no Java SDK, no webhooks
- **Football-Data.org**: Free tier but no live scores, limited events
- **Sportradar/Opta**: Enterprise-only, custom pricing, overkill

### Implementation
```java
// Abstraction layer for provider independence
public interface FootballDataProvider {
    List<Competition> getCompetitions();
    List<Fixture> getFixtures(CompetitionId, DateRange);
    List<Match> getLiveMatches();
    MatchDetails getMatchDetails(MatchId);
    List<Standing> getStandings(CompetitionId, SeasonId);
    List<Event> getEvents(MatchId);
}

@Component
@ConditionalOnProperty(name = "football.provider", havingValue = "sportmonks")
public class SportmonksProvider implements FootballDataProvider { ... }
```

---

## 2. WhatsApp Integration: Cloud API (Official)

### Decision
Use **WhatsApp Business Platform Cloud API** (Meta-hosted) via REST + Webhooks.

### Rationale
- Official Meta-hosted infrastructure (no self-hosted containers)
- Utility template category for match notifications (~$0.0042/message in India)
- Webhook-based delivery tracking
- Template-based messaging (required for outbound notifications)
- Pair rate limits manageable with queue + exponential backoff
- Test resources for development

### Template Strategy
4 Utility Templates:
1. `match_starting_soon` - 15 min reminder
2. `goal_notification` - Goal events
3. `match_event` - Cards, substitutions, HT, FT
4. `full_time_result` - Match completion

### Implementation
```java
@Service
public class WhatsAppNotificationService {
    // Resilience4j: retry (3x), circuit breaker, rate limiter
    // Template variable binding
    // Webhook signature verification
    // Idempotent status updates via WAMID
}
```

---

## 3. Backend Framework: Spring Boot 3.x (Java 21)

### Decision
Spring Boot 3.4+ with Java 21 (LTS), Maven build.

### Rationale
- Enterprise-grade ecosystem for reliability requirements
- Spring Data JPA + Flyway for database management
- Spring Security + JWT for authentication
- Spring Scheduling + `@Async` for background jobs
- Resilience4j for fault tolerance (retry, circuit breaker)
- Micrometer + Actuator for observability
- Virtual threads (Project Loom) for scalable concurrency
- Strong typing for domain modeling

### Key Dependencies
```xml
<dependencies>
    <!-- Core -->
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-boot-starter-data-jpa</dependency>
    <dependency>spring-boot-starter-security</dependency>
    <dependency>spring-boot-starter-validation</dependency>
    <dependency>spring-boot-starter-actuator</dependency>
    
    <!-- Database -->
    <dependency>org.postgresql:postgresql</dependency>
    <dependency>org.flywaydb:flyway-core</dependency>
    
    <!-- Cache/Queue -->
    <dependency>spring-boot-starter-data-redis</dependency>
    <dependency>org.redisson:redisson</dependency> <!-- Distributed locks -->
    
    <!-- Resilience -->
    <dependency>io.github.resilience4j:resilience4j-spring-boot3</dependency>
    <dependency>io.github.resilience4j:resilience4j-retry</dependency>
    <dependency>io.github.resilience4j:resilience4j-circuitbreaker</dependency>
    
    <!-- Scheduling -->
    <dependency>net.javacrumbs.shedlock:shedlock-spring</dependency>
    <dependency>net.javacrumbs.shedlock:shedlock-provider-jdbc-template</dependency>
    
    <!-- Testing -->
    <dependency>spring-boot-starter-test</dependency>
    <dependency>org.testcontainers:postgresql</dependency>
    <dependency>org.testcontainers:redis</dependency>
    <dependency>org.testcontainers:junit-jupiter</dependency>
</dependencies>
```

---

## 4. Database: PostgreSQL 16 (Primary Source of Truth)

### Decision
PostgreSQL as the sole durable store. Redis for caching, queue coordination, and distributed locks only.

### Rationale
- ACID compliance for notification state, favorites, user data
- JSONB for flexible provider response caching
- Advisory locks / `SELECT FOR UPDATE SKIP LOCKED` for queue processing
- Flyway for versioned migrations
- Railway/Render managed service with automated backups

### Schema Design Principles
- UTC timestamps everywhere (`TIMESTAMPTZ`)
- UUID primary keys for external references
- Provider-specific IDs stored separately (`provider_match_id`)
- Optimistic locking (`@Version`) for concurrent updates
- Soft deletes for audit trail

### Core Entities
```sql
-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    whatsapp_opt_in BOOLEAN DEFAULT FALSE,
    opt_in_at TIMESTAMPTZ,
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Competitions (static reference data)
CREATE TABLE competitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_competition_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    logo_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE
);

-- Teams
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_team_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    short_name VARCHAR(20),
    logo_url VARCHAR(500),
    competition_id UUID REFERENCES competitions(id)
);

-- Matches (core entity)
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_match_id VARCHAR(50) UNIQUE NOT NULL,
    competition_id UUID NOT NULL REFERENCES competitions(id),
    season_id VARCHAR(50),
    home_team_id UUID NOT NULL REFERENCES teams(id),
    away_team_id UUID NOT NULL REFERENCES teams(id),
    scheduled_start TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL, -- SCHEDULED, LIVE, HALFTIME, FINISHED, POSTPONED, CANCELLED
    home_score INTEGER DEFAULT 0,
    away_score INTEGER DEFAULT 0,
    match_minute INTEGER,
    last_provider_update TIMESTAMPTZ,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Match Events (goals, cards, subs, etc.)
CREATE TABLE match_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id),
    provider_event_id VARCHAR(50),
    event_type VARCHAR(30) NOT NULL, -- GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION, HALFTIME, FULLTIME, etc.
    team_id UUID REFERENCES teams(id),
    player_id UUID,
    minute INTEGER,
    extra_minute INTEGER,
    description TEXT,
    metadata JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(match_id, provider_event_id) -- Idempotency
);

-- User Favorites (matches)
CREATE TABLE favorite_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    match_id UUID NOT NULL REFERENCES matches(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, match_id)
);

-- User Favorites (teams)
CREATE TABLE favorite_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    team_id UUID NOT NULL REFERENCES teams(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, team_id)
);

-- Notification Preferences
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    match_start_reminder BOOLEAN DEFAULT TRUE,
    goal_notifications BOOLEAN DEFAULT TRUE,
    card_notifications BOOLEAN DEFAULT FALSE,
    substitution_notifications BOOLEAN DEFAULT FALSE,
    half_time_notifications BOOLEAN DEFAULT FALSE,
    full_time_notifications BOOLEAN DEFAULT TRUE,
    reminder_minutes_before INTEGER DEFAULT 15,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Notifications (audit log)
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    match_id UUID REFERENCES matches(id),
    event_id UUID REFERENCES match_events(id),
    type VARCHAR(30) NOT NULL, -- MATCH_START, GOAL, CARD, SUBSTITUTION, HALFTIME, FULLTIME
    template_name VARCHAR(100),
    status VARCHAR(20) NOT NULL, -- PENDING, PROCESSING, SENT, DELIVERED, FAILED, DEAD_LETTER
    whatsapp_message_id VARCHAR(128), -- WAMID
    attempt_count INTEGER DEFAULT 0,
    last_error TEXT,
    scheduled_for TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Notification Attempts (retry tracking)
CREATE TABLE notification_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    response_body JSONB,
    attempted_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_matches_status_scheduled ON matches(status, scheduled_start);
CREATE INDEX idx_matches_live ON matches(status) WHERE status IN ('LIVE', 'HALFTIME');
CREATE INDEX idx_favorite_matches_user ON favorite_matches(user_id);
CREATE INDEX idx_favorite_teams_user ON favorite_teams(user_id);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notification_attempts_notification ON notification_attempts(notification_id);
```

---

## 5. Redis Usage: Cache, Queue, Locks (Not Source of Truth)

### Decision
Redis for:
- Live match data cache (TTL: 60s)
- Notification queue (Redis Streams or List + BLMOVE)
- Distributed locks (Redisson) for scheduler coordination
- Deduplication cache (event fingerprints)
- Rate limiting counters

### Not For
- Primary notification state (PostgreSQL)
- User preferences (PostgreSQL)
- Match/event persistence (PostgreSQL)

### Configuration
```yaml
spring:
  data:
    redis:
      url: ${REDIS_URL}
      timeout: 5s
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

---

## 6. Match Synchronization Engine

### Decision
Polling-based with intelligent frequency + Webhook migration path.

### Polling Strategy
| Match State | Frequency | Matches Selected |
|-------------|-----------|------------------|
| LIVE, HALFTIME | 30 seconds | Currently live |
| SCHEDULED (≤24h) | 5 minutes | Upcoming today/tomorrow |
| SCHEDULED (>24h) | 15 minutes | This week |
| FINISHED (recent) | 1 hour | Last 24h (for corrections) |
| Others | Daily | Full sync |

### State Reconciliation
```java
@Service
public class MatchSyncService {
    public void syncMatches() {
        // 1. Fetch live + upcoming matches from provider
        // 2. Normalize to internal domain model
        // 3. Compare with stored state (by provider_match_id)
        // 4. Detect changes: status, score, events
        // 5. Persist new state (optimistic locking)
        // 6. Generate domain events for changes
        // 7. Publish to notification queue
    }
}
```

### Restart Recovery
- On startup: Fetch current live/upcoming matches from provider
- Compare with stored state
- **Do not** generate events for historical goals
- Only notify on *new* changes detected after restart

---

## 7. Event Detection & Idempotency

### Decision
Deterministic event keys + database unique constraints.

### Event Key Formula
```java
String eventKey = matchId + ":" + eventType + ":" + providerEventId;
// If provider doesn't provide event ID:
String eventKey = matchId + ":" + eventType + ":" + teamId + ":" + minute + ":" + extraMinute;
```

### Deduplication Flow
```
Provider Event
      ↓
Generate Event Key
      ↓
INSERT INTO match_events ... ON CONFLICT (match_id, provider_event_id) DO NOTHING
      ↓
If INSERT succeeded (new event):
    → Find users with favorite match/team
    → Create notification jobs
    → Push to Redis queue
Else (duplicate):
    → Ignore silently
```

### Supported Events (Sportmonks)
- GOAL (including own goals, penalties)
- YELLOW_CARD
- RED_CARD
- SUBSTITUTION
- HALFTIME
- FULLTIME
- EXTRA_TIME_START
- PENALTY_SHOOTOUT
- MATCH_STARTED (status change)
- POSTPONED
- CANCELLED

---

## 8. Notification Queue & Retry

### Decision
Redis Streams for queue + PostgreSQL for durable state + Background worker.

### Architecture
```
Match Event Detected
         ↓
Create Notification Records (PENDING) in PostgreSQL
         ↓
Push Notification IDs to Redis Stream (notification:queue)
         ↓
Worker Consumes (XREADGROUP BLOCK)
         ↓
Load Notification from PostgreSQL
         ↓
Send via WhatsApp API (Resilience4j retry)
         ↓
Update Notification Status (SENT/FAILED)
         ↓
On FAILURE: Increment attempt, schedule retry with backoff
         ↓
After 3 failures: DEAD_LETTER, alert
```

### Retry Policy
| Attempt | Delay | Max Delay |
|---------|-------|-----------|
| 1 | 5 seconds | - |
| 2 | 10 seconds | - |
| 3 | 30 seconds | - |
| 4 | 60 seconds | - |
| **Total: 4 attempts** | **Max ~2 min** | **Dead letter after** |

---

## 9. Frontend: React 18 + Vite + Tailwind CSS

### Decision
Minimal SPA with React Router, deployed to Vercel.

### Pages
1. **Home** - Competition selector, live matches summary
2. **Competition** - Matches grouped by LIVE / UPCOMING / FINISHED
3. **Match Detail** - Score, events timeline, star/unstar button
4. **Favorites** - User's starred matches & teams
5. **Settings** - Notification preferences, phone number, timezone

### State Management
- React Query (TanStack Query) for server state
- Local component state for UI
- No global state library needed (simple app)

### API Client
```typescript
// Auto-generated from OpenAPI spec or manual
const api = {
  competitions: () => GET('/api/competitions'),
  matches: (competitionId) => GET(`/api/competitions/${competitionId}/matches`),
  liveMatches: () => GET('/api/matches/live'),
  matchDetail: (id) => GET(`/api/matches/${id}`),
  favoriteMatch: (id) => POST(`/api/matches/${id}/favorite`),
  unfavoriteMatch: (id) => DELETE(`/api/matches/${id}/favorite`),
  preferences: () => GET('/api/notification-preferences'),
  updatePreferences: (prefs) => PUT('/api/notification-preferences', prefs),
};
```

---

## 10. Authentication: JWT + Phone Number

### Decision
Simple phone-number-based auth with JWT tokens.

### Flow
1. User enters phone number on frontend
2. Backend sends WhatsApp OTP (Authentication template)
3. User enters OTP
4. Backend validates, returns JWT
5. JWT stored in httpOnly cookie (or localStorage for simplicity)
6. JWT included in Authorization header for API calls

### Token Config
- Access token: 15 min (short-lived)
- Refresh token: 30 days (httpOnly cookie)
- HS256 signing with `JWT_SECRET` from env

---

## 11. Timezone Handling

### Decision
- Store all timestamps in UTC (PostgreSQL `TIMESTAMPTZ`)
- User timezone in `users.timezone` (default: `Asia/Kolkata`)
- Convert at boundary: API responses, WhatsApp templates
- Match start reminders calculated in user's timezone

---

## 12. Security Requirements

### Mandatory
- [ ] No secrets in code/git (`.env.example` only)
- [ ] Environment variables for all credentials
- [ ] Spring Security with JWT validation
- [ ] CORS restricted to production frontend origin
- [ ] HTTPS enforced (platform-terminated)
- [ ] Input validation on all endpoints
- [ ] Rate limiting on auth endpoints
- [ ] Webhook signature verification (WhatsApp)
- [ ] Password hashing (BCrypt) - if password auth added
- [ ] Audit logging for notifications

### Secret Checklist
| Secret | Location |
|--------|----------|
| `JWT_SECRET` | Railway Variables |
| `SPORTMONKS_API_TOKEN` | Railway Variables |
| `WHATSAPP_ACCESS_TOKEN` | Railway Variables |
| `WHATSAPP_PHONE_NUMBER_ID` | Railway Variables |
| `WHATSAPP_WABA_ID` | Railway Variables |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Railway Variables |
| `WHATSAPP_APP_SECRET` | Railway Variables |
| `DATABASE_URL` | Railway (auto) |
| `REDIS_URL` | Railway (auto) |

---

## 13. Testing Strategy

### Unit Tests (JUnit 5 + Mockito)
- Event detection logic
- Score comparison
- Match status transitions
- Event deduplication keys
- Notification creation
- Retry calculation
- Favorite logic
- Timezone conversions

### Integration Tests (Testcontainers)
- Football provider → Match sync → Database
- Match sync → Event detection → Notification queue
- Notification worker → WhatsApp API (mocked)
- Webhook processing → Status updates
- Full restart recovery scenario

### Failure Tests
- Provider timeout / invalid data
- Duplicate event handling
- Server restart mid-match
- WhatsApp timeout / 4xx / 5xx
- Rate limiting
- Database connection failure
- Redis failure (degraded mode)

---

## 14. Definition of Done

The system is complete when:

1. **User selects match** → Backend stores favorite
2. **User closes app** → Backend continues monitoring
3. **Match approaches** → WhatsApp reminder sent (15 min before)
4. **Match starts** → Backend detects LIVE status
5. **Goal occurs** → Backend detects score change
6. **WhatsApp notification sent** → Within 30 seconds
7. **Same event re-polled** → Duplicate prevented
8. **Match ends** → Full-time notification sent
9. **Backend restarts** → No historical replay, continues monitoring
10. **API outage** → Graceful degradation, auto-recovery
11. **WhatsApp failure** → Queued, retried, not lost
12. **Secrets secure** → None in git, none in frontend, none in logs

---

## 15. Milestones

| Milestone | Deliverables | Target |
|-----------|--------------|--------|
| M1: Research | API comparison, WhatsApp research, deployment research, ADR | Week 1 |
| M2: Backend Foundation | Spring Boot, PG, Redis, Config, Security, Logging | Week 2 |
| M3: Football Data | Provider integration, competitions, teams, fixtures, live, standings | Week 3 |
| M4: Live Engine | Scheduler, sync, state comparison, event detection, deduplication | Week 4 |
| M5: Favorites | Auth, favorite match/team, notification preferences | Week 5 |
| M6: WhatsApp | Cloud API integration, templates, queue, retry, delivery tracking | Week 6 |
| M7: Frontend | Competition list, match list, live score, details, star/unstar | Week 7 |
| M8: Reliability | Tests, failure sim, restart test, duplicate test, security review | Week 8 |
| M9: Deployment | Prod PG, Redis, backend deploy, Vercel frontend, secrets, smoke tests | Week 9 |