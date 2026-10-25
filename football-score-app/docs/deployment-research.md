# Deployment Architecture Research (2026)

## Executive Summary

**Recommended Architecture:**
- **Frontend**: Vercel (React/Vite + Tailwind) - Free tier sufficient
- **Backend**: Railway or Render - Managed PostgreSQL, Redis, long-running Spring Boot, cron jobs
- **Database**: Managed PostgreSQL (included with backend platform)
- **Cache/Queue**: Managed Redis (included with backend platform)
- **Secrets**: Platform secret management (never in code)

## Backend Hosting Comparison (Spring Boot + PostgreSQL + Redis + Scheduled Jobs)

| Platform | Spring Boot Support | Managed PG | Managed Redis | Cron/Workers | Min Cost | Free Tier | Notes |
|----------|---------------------|------------|---------------|--------------|----------|-----------|-------|
| **Railway** | ✅ Native (Maven/Gradle) | ✅ | ✅ | ✅ Cron jobs + workers | $5/mo | $5 credit/mo | Best DX, single project model |
| **Render** | ✅ Native | ✅ | ✅ | ✅ Cron jobs + background workers | $7/mo | Spins down | Simple, predictable pricing |
| **Fly.io** | ✅ Docker required | Self-managed app | Self-managed app | ✅ Machines | ~$5/mo | 30-day trial | More ops overhead |
| **Koyeb** | ✅ Native | ✅ | ✅ | ✅ | $5.50/mo | Limited | Good alternative |
| **Granite** | ✅ Native | ✅ | ✅ | ✅ Cron jobs | $5/mo | 30-day trial | Spring-optimized |

### Recommendation: **Railway**

**Reasons:**
1. **Single project model** - Backend, PostgreSQL, Redis, cron jobs in one project
2. **Native Spring Boot support** - Detects Maven/Gradle, builds without Dockerfile
3. **Cron jobs as first-class service type** - Perfect for match synchronization scheduler
4. **Background workers** - Separate deployable for notification processing
5. **Managed PostgreSQL + Redis** - Private networking, automated backups
6. **Environment promotion** - Staging → Production with separate secrets
7. **MCP server for agents** - Programmable deployment
8. **Cost-effective** - $5/month Hobby includes $5 usage credit

### Alternative: **Render**

If Railway has issues:
- Starter: $7/mo (512 MB RAM - tight for JVM)
- Standard: $25/mo (1 GB RAM - comfortable)
- Cron jobs and background workers supported

## Frontend Hosting: Vercel

**Why Vercel:**
- Native Vite + React + Tailwind support
- Free tier: 100 GB bandwidth, unlimited personal projects
- Automatic HTTPS, custom domains
- Edge network, fast global CDN
- Preview deployments for every git push
- Environment variables for API URL

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        VERCEL                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  React + Vite + Tailwind (Static + SSR optional)    │    │
│  │  Environment: VITE_API_URL=https://api.example.com  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                    HTTPS / REST API
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        RAILWAY PROJECT                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Web Service │  │  Cron Job    │  │  Worker      │       │
│  │  (Spring Boot)│  │  (Scheduler) │  │  (Notifications)│     │
│  │  Port 8080   │  │  @Scheduled  │  │  @Async/Queue │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │                │
│         └─────────────────┼─────────────────┘                │
│                           │                                  │
│         ┌─────────────────┼─────────────────┐                │
│         ▼                 ▼                 ▼                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  PostgreSQL  │  │    Redis     │  │   Secrets    │       │
│  │  (Managed)   │  │  (Managed)   │  │  (Platform)  │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
       ┌────────────┐ ┌────────────┐ ┌────────────┐
       │ Sportmonks │ │  WhatsApp  │ │  Monitoring│
       │    API     │ │ Business   │ │  (Logs/    │
       │            │ │   API      │ │  Metrics)  │
       └────────────┘ └────────────┘ └────────────┘
```

## Railway Service Configuration

### 1. Web Service (Spring Boot API)

```yaml
# railway.toml (optional, auto-detected)
[build]
builder = "nixpacks"
# Auto-detects Maven/Gradle, runs: ./mvnw package -DskipTests

[deploy]
startCommand = "java -jar target/football-score-app-0.0.1-SNAPSHOT.jar"
healthcheckPath = "/actuator/health"
healthcheckTimeout = 60
restartPolicyType = "on_failure"
restartPolicyMaxRetries = 10

[env]
SPRING_PROFILES_ACTIVE = "production"
SERVER_PORT = "8080"
# Injected from Railway variables:
# DATABASE_URL, REDIS_URL, SPORTMONKS_API_TOKEN, WHATSAPP_*
```

**Resource Requirements:**
- JVM: `-Xms256m -Xmx512m` (Hobby: 512 MB RAM)
- CPU: 0.5 vCPU (shared)
- Scale: 1 replica (Hobby), horizontal on Pro

### 2. Cron Job (Match Synchronization)

```yaml
# Separate service in same project
[deploy]
startCommand = "java -jar target/football-score-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=scheduler"
cronSchedule = "*/1 * * * *"  # Every minute (adjust based on rate limits)
```

**Note**: Railway cron jobs run the same image with different entrypoint. Use profile to activate only scheduler beans.

### 3. Background Worker (Notification Processing)

```yaml
[deploy]
startCommand = "java -jar target/football-score-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=worker"
# Processes notification queue from Redis
# Horizontal scaling supported on Pro
```

## Environment Variables (Railway)

### Production Secrets (Set in Railway Dashboard → Variables)

```bash
# Database (Auto-injected by Railway PostgreSQL)
DATABASE_URL=postgresql://user:pass@host:5432/db

# Redis (Auto-injected by Railway Redis)
REDIS_URL=redis://user:pass@host:6379

# Application
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=<generate-secure-random-256-bit>
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10

# Football Provider
SPORTMONKS_API_TOKEN=<from-sportmonks-dashboard>
SPORTMONKS_BASE_URL=https://api.sportmonks.com/v3/football

# WhatsApp
WHATSAPP_ACCESS_TOKEN=<system-user-token>
WHATSAPP_PHONE_NUMBER_ID=<from-whatsapp-manager>
WHATSAPP_WABA_ID=<from-whatsapp-manager>
WHATSAPP_WEBHOOK_VERIFY_TOKEN=<random-string>
WHATSAPP_APP_SECRET=<from-meta-app-dashboard>

# Frontend URL (for CORS)
FRONTEND_URL=https://football-score-app.vercel.app

# Scheduler
MATCH_START_REMINDER_MINUTES=15
LIVE_MATCH_POLL_INTERVAL_SECONDS=30
UPCOMING_MATCH_POLL_INTERVAL_MINUTES=5
```

### Development (Local `.env` - Never Committed)

```bash
# Local PostgreSQL (Docker)
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/football_score

# Local Redis (Docker)
REDIS_URL=redis://localhost:6379

# Test credentials
SPORTMONKS_API_TOKEN=<dev-token>
WHATSAPP_ACCESS_TOKEN=<test-token>
WHATSAPP_PHONE_NUMBER_ID=<test-phone-id>
WHATSAPP_WEBHOOK_VERIFY_TOKEN=dev-verify-token
WHATSAPP_APP_SECRET=<test-app-secret>

FRONTEND_URL=http://localhost:5173
```

## Database Migration Strategy

### Flyway on Railway

```properties
# application-production.properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
```

- Migrations run automatically on startup
- Railway provides `DATABASE_URL` - Spring Boot parses automatically
- Use `pg_dump` for backups (Railway does daily automated backups)

## CI/CD Pipeline

### GitHub Actions → Railway

```yaml
# .github/workflows/deploy.yml
name: Deploy to Railway

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          
      - name: Build with Maven
        run: ./mvnw -B package -DskipTests -pl backend
        
      - name: Deploy to Railway
        uses: railwayapp/railway-deploy@v1
        with:
          token: ${{ secrets.RAILWAY_TOKEN }}
          service: football-score-api
          environment: production
```

### Frontend Deploy (Vercel)

```yaml
# .github/workflows/frontend.yml
name: Deploy Frontend to Vercel

on:
  push:
    branches: [main]
    paths: ['frontend/**']

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: ./frontend
```

## Monitoring & Observability

### Railway Built-in
- Logs: Real-time streaming, 30-day retention
- Metrics: CPU, Memory, Network, Disk
- Deployments: History, rollback

### Application-Level (Spring Boot Actuator + Micrometer)

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.prometheus.metrics.export.enabled=true
```

**Grafana Cloud Free Tier** (or self-hosted on Railway):
- Prometheus metrics scraping
- Loki log aggregation
- Pre-built Spring Boot dashboards

### Health Checks

```java
@Component
public class FootballProviderHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check Sportmonks API connectivity
        // Check WhatsApp API connectivity
        // Check database connectivity
        // Check Redis connectivity
    }
}
```

## Security Configuration

### CORS (Production)

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${FRONTEND_URL}") String frontendUrl) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### HTTPS Enforcement

- Railway: Automatic TLS termination at edge
- Vercel: Automatic HTTPS
- Spring Boot: `server.ssl.enabled=false` (terminated at platform)

### Secret Management

| Secret | Where Stored | Rotation |
|--------|--------------|----------|
| `JWT_SECRET` | Railway Variables | Manual (annually) |
| `SPORTMONKS_API_TOKEN` | Railway Variables | When rotated by provider |
| `WHATSAPP_ACCESS_TOKEN` | Railway Variables | Auto-refresh via code |
| `WHATSAPP_APP_SECRET` | Railway Variables | When rotated in Meta |
| `DATABASE_URL` | Railway (auto) | Railway managed |
| `REDIS_URL` | Railway (auto) | Railway managed |

## Local Development Setup

```yaml
# docker-compose.yml (for local dev only)
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: football_score
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]
    volumes: [postgres_data:/var/lib/postgresql/data]
    
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    volumes: [redis_data:/data]

volumes:
  postgres_data:
  redis_data:
```

```bash
# Start local infrastructure
docker-compose up -d

# Run backend
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run frontend
cd frontend && npm run dev
```

## Cost Estimation (Monthly, Production)

| Component | Platform | Est. Cost |
|-----------|----------|-----------|
| Backend (Web + Cron + Worker) | Railway Hobby | $5.00 |
| PostgreSQL (1 GB) | Railway (included) | $0.00 |
| Redis (256 MB) | Railway (included) | $0.00 |
| Frontend | Vercel Free | $0.00 |
| Sportmonks API | Sportmonks Starter | €29.00 (~$31) |
| WhatsApp Messages | Meta | ~$1.00 |
| **Total** | | **~$37/month** |

## Disaster Recovery

### Backup Strategy

1. **PostgreSQL**: Railway daily automated backups (7-day retention)
   - Manual: `railway run pg_dump $DATABASE_URL > backup.sql`
   
2. **Redis**: Not backed up (ephemeral cache/queue)
   - Queue jobs persisted in PostgreSQL first

3. **Application Code**: GitHub (source of truth)

### Recovery Procedure

1. Provision new Railway project
2. Restore PostgreSQL from backup
3. Deploy backend (Flyway runs migrations)
4. Deploy frontend
5. Update DNS / Vercel domain
6. Verify WhatsApp webhook URL

## Next Steps

1. Create Railway account
2. Create new project
3. Add PostgreSQL service
4. Add Redis service
5. Deploy backend (web service)
6. Add cron job service
7. Add worker service
8. Configure environment variables
9. Set up custom domain (optional)
10. Create Vercel project for frontend
11. Configure `VITE_API_URL` in Vercel
12. Set up GitHub Actions for CI/CD
13. Configure monitoring (Grafana Cloud free tier)