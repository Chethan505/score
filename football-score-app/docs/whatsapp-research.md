# WhatsApp Business Platform Research (2026)

## Executive Summary

The **WhatsApp Business Platform Cloud API** is the recommended integration method. It provides official Meta-hosted infrastructure, eliminating the need for self-hosted on-premises containers.

## Current 2026 Requirements

### Prerequisites

1. **Meta Business Account** (Business Portfolio)
   - Must be verified for production messaging limits
   - Unverified: 250 template limit, lower throughput

2. **WhatsApp Business Account (WABA)**
   - Created within the Business Portfolio
   - Contains phone numbers and message templates

3. **Business Phone Number**
   - Real or virtual number not currently active on WhatsApp
   - Must pass display name review for Official Business Account status
   - Display name: 3-30 characters, reflects business identity

4. **Message Templates** (Required for outbound notifications)
   - Must be pre-approved by Meta
   - Categories: **Utility** (match reminders, score updates), **Authentication**, **Marketing**
   - For this project: **Utility** templates for match-start, goal, event notifications

5. **User Opt-in**
   - Explicit consent required before sending template messages
   - Must clarify business name and intent
   - Store opt-in timestamp and method

### Authentication

- **OAuth Access Tokens** (not OAuth 2.0)
- System User access token with `whatsapp_business_messaging` permission
- Token rotation via Meta's token management
- Never expose tokens to frontend

## Pricing (Effective October 1, 2026)

### Message Categories & Rates (India Market)

| Category | Rate per Message | Notes |
|----------|------------------|-------|
| **Utility** | ~$0.0042 (₹0.35) | Match reminders, score updates - **our primary category** |
| Authentication | ~$0.0135 | OTP, verification |
| Marketing | ~$0.025 | Promotional (paused for US numbers) |
| Service | First 1,000 free/month, then ~$0.0034 | User-initiated conversations |

### Volume Tiers (Utility & Authentication only)

- Tiered pricing based on monthly volume per market-category
- Aggregated at Business Portfolio level

### Cost Estimation for This Project

Assuming 1 user, ~50 matches followed per season:
- Match-start reminders: 50 × 1 = 50 messages
- Goals/events: ~150 messages (avg 3 goals/match)
- Total: ~200 utility messages/month
- **Monthly cost: ~$0.84 (₹70)**

Very affordable for personal use.

## Rate Limits (2026)

### API Request Limits

| Endpoint Type | Default Limit | Active WABA Limit |
|---------------|---------------|-------------------|
| Management (templates, numbers) | 200/hr | 5,000/hr |
| Sending messages | Per phone number throughput | |

### Message Throughput

- **Default**: 80 messages/second per phone number
- **Pair rate limit**: 1 message per 6 seconds to same user (0.17 msg/sec)
- **Burst allowance**: 45 messages in 6 seconds (borrows from future quota)
- **Retry strategy**: Exponential backoff (4^X seconds, X=0,1,2...)

### Template Limits

- Unverified business: 250 templates max
- Verified business: 6,000 templates max
- 100 templates/hour creation rate limit

## Template Design for This Project

### Required Templates (Utility Category)

#### 1. Match Starting Soon
```
Name: match_starting_soon
Language: en
Category: UTILITY
Components:
  HEADER: ⚽ Match Starting Soon
  BODY: {{team_home}} vs {{team_away}} starts in {{minutes}} minutes ({{league}})
  BUTTON: Quick Reply - "View Match"
```

#### 2. Goal Notification
```
Name: goal_notification
Language: en
Category: UTILITY
Components:
  HEADER: 🔴 GOAL!
  BODY: {{team_scoring}} {{score_home}} - {{score_away}} {{team_conceding}} ({{minute}}')
  BUTTON: Quick Reply - "View Match"
```

#### 3. Match Event (Generic - Cards, Subs, HT, FT)
```
Name: match_event
Language: en
Category: UTILITY
Components:
  HEADER: {{event_icon}} {{event_type}}
  BODY: {{team_home}} {{score_home}} - {{score_away}} {{team_away}} ({{minute}}')
  BUTTON: Quick Reply - "View Match"
```

#### 4. Full Time Result
```
Name: full_time_result
Language: en
Category: UTILITY
Components:
  HEADER: ⏹️ Full Time
  BODY: {{team_home}} {{score_home}} - {{score_away}} {{team_away}} ({{league}})
  BUTTON: Quick Reply - "View Match"
```

### Template Variables Mapping

| Variable | Description | Example |
|----------|-------------|---------|
| `{{team_home}}` | Home team name | "Arsenal" |
| `{{team_away}}` | Away team name | "Chelsea" |
| `{{league}}` | Competition name | "Premier League" |
| `{{minutes}}` | Minutes until kickoff | "15" |
| `{{team_scoring}}` | Team that scored | "Arsenal" |
| `{{score_home}}` | Home team score | "1" |
| `{{score_away}}` | Away team score | "0" |
| `{{team_conceding}}` | Team conceded | "Chelsea" |
| `{{minute}}` | Match minute | "32" |
| `{{event_icon}}` | Emoji for event | "🟨", "🔴", "🔄" |
| `{{event_type}}` | Event description | "Yellow Card", "Substitution" |

## Webhooks (Critical for Delivery Tracking)

### Required Webhook Events

1. **Message Status Updates**
   - `sent` → `delivered` → `read`
   - `failed` (with error code)
   
2. **Incoming Messages** (for opt-in, user replies)

3. **Template Status** (approved/rejected)

### Webhook Configuration

- HTTPS endpoint required (no self-signed certs)
- Verify token for challenge validation
- Retry: Meta retries failed deliveries with exponential backoff
- Idempotency: Use `message_id` (WAMID) for deduplication

### Webhook Payload Example

```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "WABA_ID",
    "changes": [{
      "value": {
        "messaging_product": "whatsapp",
        "metadata": {"display_phone_number": "15551234567"},
        "statuses": [{
          "id": "wamid.HBgMOTE...",
          "status": "delivered",
          "timestamp": "1704067200",
          "recipient_id": "919876543210",
          "conversation": {"id": "conversation_id", "expiration_timestamp": "1704153600"},
          "pricing": {"billable": true, "category": "utility", "pricing_model": "CBP"}
        }]
      },
      "field": "messages"
    }]
  }]
}
```

## Error Codes & Handling

| Code | Description | Action |
|------|-------------|--------|
| 131056 | Pair rate limit exceeded | Exponential backoff retry |
| 131042 | Media download error | Retry with new media handle |
| 131000 | Generic error | Log, alert, retry |
| 1005 | Template not found | Verify template name/status |
| 131047 | Invalid template parameters | Validate variable mapping |
| 131031 | Opt-in required | Cannot send, user hasn't opted in |

## Implementation Architecture

### Backend-Only Credentials

```
Environment Variables (Backend Only):
- WHATSAPP_ACCESS_TOKEN
- WHATSAPP_PHONE_NUMBER_ID
- WHATSAPP_WABA_ID
- WHATSAPP_WEBHOOK_VERIFY_TOKEN
- WHATSAPP_APP_SECRET (for signature verification)
```

### Message Sending Flow

```
Notification Job
       ↓
Template Selection + Variable Binding
       ↓
WhatsApp API Client (Resilience4j: retry, circuit breaker)
       ↓
POST /{phone_number_id}/messages
       ↓
Webhook receives status updates
       ↓
Update NotificationAttempt status (SENT/DELIVERED/FAILED)
```

### Retry Policy (Exponential Backoff)

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 5000, multiplier = 2.0, maxDelay = 60000),
    include = { WhatsAppApiException.class, ConnectTimeoutException.class },
    exclude = { TemplateNotFoundException.class, OptInRequiredException.class }
)
public void sendWhatsAppMessage(NotificationJob job) { ... }
```

- Max 3 retries (4 attempts total)
- Base delay: 5 seconds
- Max delay: 60 seconds
- Dead letter queue after exhaustion

## Security Requirements

1. **Never commit tokens** - Use environment variables / secret manager
2. **Verify webhook signatures** - Use `X-Hub-Signature-256` header with app secret
3. **Restrict webhook endpoint** - Only accept from Meta IPs (or verify signature)
4. **Token rotation** - Implement automatic refresh before expiry
5. **Opt-in enforcement** - Check user consent before every template send
6. **Rate limiting** - Respect pair limits per recipient

## Development vs Production

| Aspect | Development | Production |
|--------|-------------|------------|
| Phone Number | Test number (Meta provided) | Verified business number |
| WABA | Test WABA | Verified WABA |
| Templates | Auto-approved (relaxed) | Manual review required |
| Limits | 5 messages/day to test numbers | Full throughput |
| Payment | Not required | Payment method on file |

## Integration Checklist

- [ ] Create Meta Business Portfolio
- [ ] Verify business (for production limits)
- [ ] Create WhatsApp Business Account
- [ ] Register business phone number
- [ ] Submit display name for review
- [ ] Create message templates (4 utility templates)
- [ ] Wait for template approval
- [ ] Configure webhook endpoint (HTTPS)
- [ ] Generate System User access token
- [ ] Store credentials in backend environment variables
- [ ] Implement webhook signature verification
- [ ] Test end-to-end with test number
- [ ] Submit for production access (if needed)

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Template rejection | Medium | High | Follow guidelines, test in dev first |
| Rate limiting | Medium | Medium | Queue + exponential backoff |
| Webhook delivery failure | Low | High | Idempotent processing, retry logic |
| Token expiry | Low | High | Auto-refresh, monitoring |
| Policy changes | Low | Medium | Monitor Meta developer blog |
| Opt-in management | Medium | High | Clear consent flow in frontend |

## Next Steps

1. Create Meta Developer account
2. Set up Business Portfolio
3. Create test WABA and phone number
4. Design and submit 4 utility templates
5. Configure webhook endpoint (ngrok for local dev)
6. Implement `WhatsAppNotificationService` with retry logic
7. Build template variable mapping
8. Write integration tests with test credentials