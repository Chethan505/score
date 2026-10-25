# Security Review Checklist

## Pre-Deployment Security Verification

### Credential Management
- [ ] No API keys, tokens, secrets in source code
- [ ] No credentials in `application.properties` or `application-*.properties` (except `.example`)
- [ ] All secrets in Railway/Platform secret manager
- [ ] `.gitignore` excludes `.env`, `application-local.properties`, `*.key`, `*.pem`
- [ ] JWT_SECRET is 256-bit random, not default
- [ ] Sportmonks API token in Railway variables only
- [ ] WhatsApp credentials in Railway variables only
- [ ] Database credentials auto-injected by Railway
- [ ] Redis credentials auto-injected by Railway

### Authentication & Authorization
- [ ] JWT access tokens expire in 15 minutes
- [ ] JWT refresh tokens expire in 30 days
- [ ] Refresh tokens stored in httpOnly cookies (or secure storage)
- [ ] Spring Security configured with stateless sessions
- [ ] All API endpoints require authentication except `/auth/**`, `/health/**`, `/webhook/**`
- [ ] `@CurrentUser` resolver validates JWT properly
- [ ] Password hashing with BCrypt (if password auth added)

### API Security
- [ ] CORS restricted to production frontend origin only
- [ ] Input validation on all endpoints (`@Valid`, `@NotNull`, etc.)
- [ ] Global exception handler doesn't leak stack traces
- [ ] Rate limiting on auth endpoints (send-otp, verify-otp)
- [ ] Request size limits configured
- [ ] Security headers (HSTS, X-Frame-Options, CSP)

### WhatsApp Security
- [ ] Webhook signature verification using `X-Hub-Signature-256`
- [ ] App Secret stored securely, not in logs
- [ ] Opt-in enforced before sending templates
- [ ] Template variables sanitized
- [ ] Pair rate limits respected (1 msg/6sec per user)
- [ ] No user phone numbers in logs

### Data Protection
- [ ] PostgreSQL: TLS enforced
- [ ] Redis: TLS enforced (if external)
- [ ] PII minimized (only phone number stored)
- [ ] UTC timestamps everywhere
- [ ] Soft deletes for audit trail
- [ ] Notification content not logged in full

### Infrastructure
- [ ] HTTPS enforced (platform terminated)
- [ ] Railway/Render private networking for DB/Redis
- [ ] Database backups enabled (daily)
- [ ] Database: least privilege user
- [ ] Redis: not publicly accessible
- [ ] Health endpoints: liveness/readiness probes configured

### Logging & Monitoring
- [ ] No secrets in logs (Spring Boot default masks passwords)
- [ ] Structured JSON logging in production
- [ ] Log level: INFO in prod, DEBUG in dev
- [ ] Actuator endpoints: only health, info, prometheus exposed
- [ ] Prometheus metrics scraped by Grafana Cloud

### Frontend Security
- [ ] No backend secrets in frontend build
- [ ] `VITE_API_URL` points to production backend
- [ ] Content Security Policy headers
- [ ] No `localStorage` for tokens (use httpOnly cookies if possible)
- [ ] XSS prevention (React auto-escapes)

### Deployment Verification
- [ ] Git history scanned for secrets (`git-secrets`, `truffleHog`)
- [ ] Docker image scanned for vulnerabilities (`trivy`)
- [ ] Dependencies scanned (`mvn dependency-check`, `npm audit`)
- [ ] Production credentials different from development
- [ ] Rollback procedure documented
- [ ] Incident response plan exists

## Post-Deployment Verification

### Smoke Tests
- [ ] User login → OTP → JWT flow works
- [ ] Competition list loads
- [ ] Match detail shows events
- [ ] Favorite match persists
- [ ] Notification preferences save
- [ ] WhatsApp webhook receives status updates
- [ ] Health endpoints return 200

### Penetration Testing (Basic)
- [ ] SQL injection attempts blocked
- [ ] XSS attempts blocked
- [ ] JWT tampering rejected
- [ ] Expired tokens rejected
- [ ] Unauthorized access returns 401/403
- [ ] Rate limiting works

### Incident Response
- [ ] Alert on dead letter queue notifications
- [ ] Alert on WhatsApp API failures
- [ ] Alert on database connection failures
- [ ] Alert on high error rates
- [ ] Runbook for credential rotation

## Credential Rotation Schedule

| Credential | Rotation Frequency | Procedure |
|------------|-------------------|-----------|
| JWT_SECRET | Annually | Generate new, deploy, invalidate sessions |
| Sportmonks API Token | When rotated by provider | Update Railway variable |
| WhatsApp Access Token | Auto-refreshed by code | Monitor for failures |
| WhatsApp App Secret | When rotated in Meta | Update Railway variable |
| Database Password | Quarterly | Railway managed |
| Redis Password | Quarterly | Railway managed |

## Emergency Procedures

### Credential Compromise
1. Immediately revoke compromised credential
2. Generate new credential
3. Update platform secret
4. Deploy if necessary
5. Verify old credential no longer works
6. Audit access logs for unauthorized usage

### Data Breach
1. Contain: disable affected systems
2. Assess: determine scope
3. Notify: users, authorities (if required)
4. Remediate: fix vulnerability
5. Document: post-mortem

---

**Sign-off**: ________________ **Date**: ________________