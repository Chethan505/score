# Deployment Checklist

## Phase 1: Local Development Complete

- [ ] All unit tests pass (`./mvnw test`)
- [ ] Integration tests pass (`./mvnw verify -Pintegration`)
- [ ] Frontend builds successfully (`npm run build`)
- [ ] Docker compose starts all services locally
- [ ] End-to-end scenario tested manually:
  - [ ] User registers with phone number
  - [ ] OTP verification works
  - [ ] Competitions load
  - [ ] Match detail shows live data
  - [ ] Favorite match star works
  - [ ] Notification preferences save
  - [ ] Match start reminder triggers (simulated)
  - [ ] Goal notification triggers (simulated)
  - [ ] Duplicate event prevention verified

## Phase 2: Infrastructure Provisioning

### Railway Project
- [ ] Create new Railway project: `football-score-prod`
- [ ] Add PostgreSQL 16 service
- [ ] Add Redis 7 service
- [ ] Verify private networking between services
- [ ] Configure automated daily backups (7-day retention)
- [ ] Set up custom domain: `api.football-score.example.com`
- [ ] Configure TLS (Railway automatic)

### Vercel Project
- [ ] Import frontend repository
- [ ] Set build command: `npm run build`
- [ ] Set output directory: `dist`
- [ ] Configure environment variable: `VITE_API_URL=https://api.football-score.example.com`
- [ ] Set custom domain: `football-score.example.com`
- [ ] Verify automatic HTTPS

### DNS Configuration
- [ ] A record: `football-score.example.com` → Vercel
- [ ] CNAME: `api.football-score.example.com` → Railway domain

## Phase 3: Backend Deployment

### Environment Variables (Railway)
```bash
# Application
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=<256-bit-secure-random>
APP_FRONTEND_URL=https://football-score.example.com

# Football Provider
SPORTMONKS_API_TOKEN=<production-token>

# WhatsApp
WHATSAPP_ACCESS_TOKEN=<production-system-user-token>
WHATSAPP_PHONE_NUMBER_ID=<production-phone-id>
WHATSAPP_WABA_ID=<production-waba-id>
WHATSAPP_WEBHOOK_VERIFY_TOKEN=<random-32-char-string>
WHATSAPP_APP_SECRET=<meta-app-secret>

# Database & Redis (Auto-injected by Railway)
# DATABASE_URL
# REDIS_URL
```

### Deployment Steps
- [ ] Push to `main` branch
- [ ] GitHub Actions builds and deploys backend
- [ ] Verify deployment logs: Flyway migrations run
- [ ] Verify health endpoint: `GET /actuator/health`
- [ ] Verify API endpoints respond
- [ ] Check scheduler starts (logs show "Starting live match sync")

### Cron Jobs (Railway Services)
- [ ] Create `scheduler` service (same image, profile `scheduler`)
- [ ] Create `worker` service (same image, profile `worker`)
- [ ] Verify both services start and connect to Redis/PostgreSQL

## Phase 4: WhatsApp Production Configuration

### Meta Business Portfolio
- [ ] Business verification complete
- [ ] WhatsApp Business Account created
- [ ] Production phone number registered
- [ ] Display name approved (Official Business Account)

### Templates (Utility Category)
- [ ] `match_starting_soon` - Approved
- [ ] `goal_notification` - Approved
- [ ] `match_event` - Approved
- [ ] `full_time_result` - Approved

### Webhook Configuration
- [ ] Webhook URL: `https://api.football-score.example.com/webhook/whatsapp`
- [ ] Verify token matches `WHATSAPP_WEBHOOK_VERIFY_TOKEN`
- [ ] Subscribe to: `messages`, `message_template_status_update`
- [ ] Test webhook delivery from Meta dashboard

### System User Token
- [ ] Create System User in Business Manager
- [ ] Assign `whatsapp_business_messaging` permission
- [ ] Generate long-lived access token
- [ ] Store in Railway `WHATSAPP_ACCESS_TOKEN`

## Phase 5: Frontend Deployment

### Environment Variables (Vercel)
```bash
VITE_API_URL=https://api.football-score.example.com
```

### Deployment
- [ ] Push to `main` branch
- [ ] Vercel builds and deploys automatically
- [ ] Verify build logs: no TypeScript errors
- [ ] Verify production URL loads
- [ ] Test login flow with production WhatsApp number

## Phase 6: Production Smoke Tests

### Authentication
- [ ] Send OTP to production phone number
- [ ] Verify OTP → receive JWT tokens
- [ ] Refresh token works
- [ ] Logout clears tokens

### Data Flow
- [ ] Competitions load (5 leagues visible)
- [ ] Match lists: Live / Upcoming / Finished tabs work
- [ ] Match detail: score, events, lineups display
- [ ] Star/unstar match works
- [ ] Favorite teams list works

### Notifications
- [ ] Notification preferences page loads
- [ ] Toggle preferences saves
- [ ] Match start reminder: simulate upcoming match
- [ ] Goal notification: simulate live match goal
- [ ] Webhook receives: sent → delivered status

### Monitoring
- [ ] Grafana Cloud: Spring Boot dashboard shows metrics
- [ ] Logs: Railway logs streaming
- [ ] Alerts: Configure for error rate > 5%

## Phase 7: Security Verification

- [ ] Run `trivy` on Docker image
- [ ] Run `mvn dependency-check` 
- [ ] Run `npm audit` (frontend)
- [ ] Verify no secrets in Git history: `git-secrets --scan-history`
- [ ] Verify `.gitignore` excludes all sensitive files
- [ ] CORS: Only `https://football-score.example.com` allowed
- [ ] HTTPS: All endpoints accessible via HTTPS only
- [ ] Actuator: Only `/health`, `/info`, `/prometheus` exposed

## Phase 8: Go-Live

- [ ] Final end-to-end test with real match
- [ ] Monitor for 30 minutes
- [ ] Verify WhatsApp notifications delivered
- [ ] Document any issues
- [ ] Update status page / notify stakeholders

---

## Rollback Plan

### Backend Rollback
1. Railway: Previous deployment → "Rollback"
2. Or: `git revert` last commit → push → auto-deploy
3. Database: Flyway doesn't support down migrations easily
   - If migration issue: restore PostgreSQL from backup
   - Railway: Daily snapshots available

### Frontend Rollback
1. Vercel: Previous deployment → "Promote to Production"
2. Or: `git revert` → push → auto-deploy

### Database Rollback
1. Railway PostgreSQL: Restore from snapshot
2. Point-in-time recovery available

---

## Post-Launch Monitoring (First 24 Hours)

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API Response Time (p95) | < 500ms | > 2s |
| Error Rate | < 0.1% | > 1% |
| WhatsApp Delivery Rate | > 98% | < 95% |
| Scheduler Lag | < 10s | > 60s |
| Queue Depth | < 100 | > 1000 |
| DB Connections | < 80% pool | > 90% |

---

## Maintenance Windows

- **Weekly**: Dependency updates (Dependabot)
- **Monthly**: Security patches, Flyway migration review
- **Quarterly**: Credential rotation, penetration test
- **Annually**: Architecture review, disaster recovery drill

---

**Deployment Lead**: ________________ **Date**: ________________
**Security Review**: ________________ **Date**: ________________