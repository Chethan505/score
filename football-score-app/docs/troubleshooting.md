# Troubleshooting Guide

## Backend Issues

### Application Won't Start

**Symptom**: Spring Boot fails to start
```
Caused by: org.postgresql.util.PSQLException: Connection refused
```

**Causes & Fixes**:
1. PostgreSQL not running
   - Local: `docker-compose up -d postgres`
   - Prod: Check Railway PostgreSQL service status
2. Wrong DATABASE_URL
   - Verify `spring.datasource.url` in environment
   - Check Railway private networking
3. Flyway migration failed
   - Check migration scripts for syntax errors
   - Run `./mvnw flyway:repair` locally (careful!)

### Database Migration Errors

**Symptom**: `FlywayException: Migration failed`
```
ERROR: relation "users" already exists
```

**Fixes**:
1. Clean database and re-run: `./mvnw flyway:clean flyway:migrate` (dev only!)
2. Check for duplicate migration version numbers
3. Verify `spring.flyway.baseline-on-migrate=true` for existing DB

### Redis Connection Issues

**Symptom**: `RedisConnectionException: Unable to connect to localhost:6379`

**Fixes**:
1. Redis not running: `docker-compose up -d redis`
2. Wrong host/port in `spring.data.redis.*`
3. Redis password required but not configured
4. Firewall blocking port 6379

### Scheduler Not Running

**Symptom**: Matches not syncing, no logs from `MatchSyncService`

**Fixes**:
1. Check `@EnableScheduling` on main class
2. Verify ShedLock table exists: `SELECT * FROM shedlock;`
3. Check for lock contention: `DELETE FROM shedlock WHERE name='syncLiveMatches';`
4. Verify `@Scheduled` methods not throwing silent exceptions
5. Check `spring.task.scheduling.pool.size` (default 1, may need more)

### Match Sync Issues

**Symptom**: Live matches not updating, scores stale

**Debugging**:
1. Check Sportmonks API quota: `GET /actuator/health` → footballProvider details
2. Verify API token valid: `curl -H "Authorization: Bearer $TOKEN" https://api.sportmonks.com/v3/football/leagues`
3. Check rate limiting: logs show 429 responses
4. Verify competition IDs match: Premier League=8, La Liga=564, Serie A=384, UCL=2, ISL=305
5. Check `last_provider_update` timestamp in matches table

### Event Detection Not Working

**Symptom**: Goals detected but no notifications sent

**Debugging**:
1. Check `match_events` table for new events
2. Verify `provider_event_id` unique constraint prevents duplicates
3. Check `notification` table for PENDING/SENT records
4. Verify `favorite_matches` / `favorite_teams` have entries for user
5. Check user `notification_preferences` for enabled notification types
6. Verify `notification_queue` has entries: `XRANGE notification:queue - + COUNT 10`

### Notification Queue Stuck

**Symptom**: Notifications stay PENDING, not sent

**Fixes**:
1. Check `NotificationWorker` running: logs show "Processing notification queue"
2. Verify Redis stream consumer group: `XINFO GROUPS notification:queue`
3. Check dead letter queue: `SELECT * FROM notifications WHERE status='DEAD_LETTER';`
4. Verify WhatsApp API credentials valid
5. Check circuit breaker state: `/actuator/circuitbreakers`

### WhatsApp API Errors

**Error 131056**: Pair rate limit exceeded
- **Fix**: Implement exponential backoff, respect 1 msg/6sec per user

**Error 1005**: Template not found
- **Fix**: Verify template name matches exactly, check approval status in Meta

**Error 131047**: Invalid template parameters
- **Fix**: Check variable mapping in `buildTemplateVariables()`, ensure all required vars provided

**Error 131031**: Opt-in required
- **Fix**: User must send message to business first, or use opt-in flow

**Error 400**: Invalid phone number format
- **Fix**: Ensure E.164 format: `+919876543210`

### Webhook Not Receiving Events

**Symptom**: WhatsApp status updates not processed

**Fixes**:
1. Verify webhook URL accessible publicly: `curl -X GET https://api.example.com/webhook/whatsapp?hub.mode=subscribe&hub.challenge=123&hub.verify_token=TOKEN`
2. Check `X-Hub-Signature-256` verification not failing
3. Check `WhatsAppWebhookController` logs for errors
4. Verify Meta dashboard shows webhook subscribed to correct events

### Duplicate Notifications

**Symptom**: User receives same goal notification multiple times

**Fixes**:
1. Check `match_events` unique constraint: `UNIQUE(match_id, provider_event_id)`
2. Verify `NotificationService.createGoalNotification()` checks existing notifications
3. Check idempotency key generation in `MatchEvent.getEventKey()`
4. Verify Redis deduplication cache if used

### Timezone Issues

**Symptom**: Match times wrong, reminders at wrong time

**Fixes**:
1. Verify database stores UTC: `SELECT scheduled_start FROM matches;`
2. Check user timezone: `SELECT timezone FROM users;`
3. Verify `ZoneOffset.UTC` used in all conversions
4. Check `format()` in frontend uses user's timezone

### Memory Issues

**Symptom**: OOM kills, container restarts

**Fixes**:
1. Increase JVM heap: `-Xmx1g` (if memory allows)
2. Check for memory leaks: `jmap -histo:live <pid>`
4. Reduce `spring.datasource.hikari.maximum-pool-size`
5. Check Redis memory: `INFO memory` → `maxmemory-policy allkeys-lru`

### High CPU Usage

**Symptom**: Container CPU at 100%

**Fixes**:
1. Check infinite loop in `@Scheduled` methods
2. Reduce poll frequency: `scheduler.live-match-poll-interval-seconds`
3. Check for excessive logging (DEBUG in prod)
4. Profile with `jcmd <pid> JFR.start`

## Frontend Issues

### Build Fails

**TypeScript Errors**
- Run `npm run build` locally first
- Check for missing types: `npm install --save-dev @types/...`

**Tailwind Not Working**
- Verify `content` paths in `tailwind.config.js`
- Run `npx tailwindcss -i ./src/index.css -o ./dist/output.css --watch`

### Runtime Errors

**API Calls Fail (CORS)**
- Verify backend CORS config allows frontend origin
- Check `Access-Control-Allow-Origin` header

**Auth Token Expired**
- Check `refreshToken` flow in `api.ts` interceptor
- Verify backend `/auth/refresh` endpoint works

**WebSocket/Real-time Not Working**
- This app uses polling, not WebSockets
- Check TanStack Query `refetchInterval` settings

## Sportmonks API Issues

### Rate Limited (429)
- Check `X-RateLimit-Remaining` header
- Implement request caching (Spring Cache + Redis)
- Reduce poll frequency for non-live matches

### Data Quality
- Missing events: Some leagues have limited event coverage
- Wrong team names: Use `provider_team_id` for matching, not names
- Delayed updates: Accept 15-30s latency for live matches

### API Changes
- Monitor Sportmonks changelog
- Version API client: pin to specific Sportmonks API version
- Test in staging before production deployment

## Database Performance

### Slow Queries
- Enable slow query log: `log_min_duration_statement = 1000`
- Add missing indexes (see migration for common ones)
- Use `EXPLAIN ANALYZE` on slow queries

### Connection Pool Exhausted
- Increase `maximum-pool-size` (max 20 for small DB)
- Fix connection leaks: ensure `@Transactional` completes
- Check for long-running transactions

## Redis Performance

### High Memory Usage
- Check key expiration: `TTL` on cache keys
- Use `SCAN` not `KEYS` in production
- Set `maxmemory-policy allkeys-lru`

### Slow Operations
- Pipeline batch operations
- Use Lua scripts for atomic operations
- Monitor `slowlog-get 10`

## Emergency Procedures

### Complete System Down
1. Check Railway/Render status page
2. Verify PostgreSQL and Redis running
3. Check application logs for startup errors
4. Restart backend service
5. If DB corrupted: restore from backup

### Data Corruption
1. Stop application immediately
2. Assess scope: `pg_dump --schema-only` vs data
3. Restore from last known good backup
4. Replay missed events from Sportmonks if possible

### WhatsApp Account Banned
1. Check Meta Business Manager for policy violations
2. Appeal if erroneous
3. Switch to backup phone number if available
4. Notify users via alternative channel

---

## Useful Commands

### Railway CLI
```bash
# View logs
railway logs -s football-score-api

# Run command in container
railway run java -jar app.jar --spring.profiles.active=production

# View variables
railway variables

# Connect to database
railway connect postgres
```

### Database
```bash
# Backup
pg_dump $DATABASE_URL > backup.sql

# Restore
psql $DATABASE_URL < backup.sql

# Check migrations
psql $DATABASE_URL -c "SELECT * FROM flyway_schema_history;"
```

### Redis
```bash
# Check queue
redis-cli XRANGE notification:queue - + COUNT 10

# Check consumer groups
redis-cli XINFO GROUPS notification:queue

# Clear queue (careful!)
redis-cli DEL notification:queue
```

### Health Checks
```bash
# Backend health
curl https://api.example.com/actuator/health

# Liveness
curl https://api.example.com/actuator/health/liveness

# Readiness
curl https://api.example.com/actuator/health/readiness
```

---

## Log Analysis Patterns

### Successful Match Sync
```
INFO  - Starting live match sync
INFO  - Fetched 3 live fixtures from provider
DEBUG - Syncing match match-123: Arsenal vs Chelsea
DEBUG - Match match-123 status: SCHEDULED -> LIVE
INFO  - Match match-123 started
DEBUG - Goal detected in match match-123: 1 - 0
DEBUG - Creating goal notification for user user-456
```

### Failed WhatsApp Delivery
```
WARN  - WhatsApp API error: 131056 (rate limited)
INFO  - Retrying notification notif-789 in 5000ms (attempt 2/4)
ERROR - Failed to send notification notif-789 after retries
INFO  - Notification notif-789 moved to dead letter queue
```

### Scheduler Lock Contention
```
WARN  - Could not obtain lock for syncLiveMatches
DEBUG - Lock held by worker-2, waiting...
INFO  - Obtained lock for syncLiveMatches
```

---

## Contact Points

| Issue | Contact |
|-------|---------|
| Railway Infrastructure | Railway Support / Discord |
| Sportmonks API | support@sportmonks.com |
| WhatsApp Business | Meta Business Support |
| Vercel Frontend | Vercel Support |
| Database Issues | Railway PostgreSQL Support |