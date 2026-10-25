# Football Data Provider Research (2026)

## Executive Summary

After evaluating legitimate football data providers for 2026, **Sportmonks** is recommended as the primary provider for this project, with **API-Football** as a viable alternative for prototyping.

## Provider Comparison

| Criteria | Sportmonks | API-Football | Football-Data.org |
|----------|-----------|--------------|-------------------|
| **League Coverage** | 2,200+ | 1,200+ | 1,000+ |
| **Required Competitions** | ✅ All 5 | ✅ All 5 | ✅ All 5 |
| **Live Scores** | < 15 sec | 15 sec | Paid only |
| **Events (Goals, Cards, Subs)** | ✅ Consistent | ✅ Varies | Limited |
| **Standings** | ✅ Real-time | ✅ | ✅ |
| **Free Tier** | 2 leagues only | 100 req/day | Free tier |
| **Entry Price** | €29/mo (5 leagues) | $19/mo (7,500 req/day) | €199/mo |
| **Uptime SLA** | 99.99% | Not published | N/A |
| **Support** | 7-day human + CS | Community forums | Minimal |
| **SDKs** | Official (Java, JS, Python, Go, etc.) | Community only | Official |
| **Webhooks** | ✅ | Limited | No |
| **Pricing Model** | Per-league tiers | Daily request caps | Monthly |

## Required Competitions Coverage

All three providers cover:
- ✅ Premier League (England)
- ✅ La Liga (Spain)
- ✅ Serie A (Italy)
- ✅ UEFA Champions League
- ✅ Indian Super League (ISL)

## Recommendation: Sportmonks

### Strengths for This Project

1. **Consistent stats coverage** across all leagues - critical for event detection (goals, cards, substitutions)
2. **99.99% uptime** with published SLA - essential for reliable notifications
3. **Official Java SDK** - native Spring Boot integration
4. **Webhook support** - enables push-based updates (reduces polling)
5. **Human support** - critical when live match issues occur
6. **Includes system** - single request fetches fixtures + scores + events + statistics
7. **Advanced metrics** (xG, Pressure Index) available as add-ons for future enhancement

### Pricing for 5 Competitions

Sportmonks Starter Plan: **€29/month** (includes 5 leagues)
- Covers exactly our 5 required competitions
- Includes all base features: live scores, events, lineups, standings
- 14-day free trial for development

### API Key Requirements

- `SPORTMONKS_API_TOKEN` - Primary authentication token
- `SPORTMONKS_BASE_URL` - `https://api.sportmonks.com/v3/football` (default)

## Alternative: API-Football

### When to Consider

- Budget-constrained prototyping ($19/mo vs €29/mo)
- Rapid MVP via RapidAPI marketplace
- Acceptable: daily request caps, community support, variable stats depth

### Limitations for Production

- Daily request caps cause failures on match days
- Inconsistent stats for lower-profile leagues
- No official Java SDK
- No published uptime SLA
- No webhook support (polling only)

## Integration Architecture

```java
// Provider abstraction - enables switching without business logic changes
public interface FootballDataProvider {
    List<Competition> getCompetitions();
    List<Fixture> getFixtures(CompetitionId, DateRange);
    List<Match> getLiveMatches();
    MatchDetails getMatchDetails(MatchId);
    List<Standing> getStandings(CompetitionId, SeasonId);
    List<Event> getEvents(MatchId);
}

// Sportmonks implementation
@Component
public class SportmonksProvider implements FootballDataProvider {
    // Uses official Java SDK or REST client
}
```

## Rate Limits & Polling Strategy

| Provider | Rate Limit | Recommended Polling |
|----------|------------|---------------------|
| Sportmonks | Hourly limits, scalable | 30-60 sec for live matches |
| API-Football | 100/day (free), 300/min (pro) | 60 sec for live matches |

**Key Principle**: Never poll every match every second. Use:
- Live matches: 30-second intervals
- Upcoming matches (next 24h): 5-minute intervals
- Other fixtures: 15-minute intervals
- Completed matches: No polling (webhook or daily sync)

## Data Model Mapping

Sportmonks v3 response → Internal domain model:

```json
// Sportmonks fixture response (simplified)
{
  "data": {
    "id": 12345,
    "league_id": 8,
    "season_id": 2024,
    "starting_at": "2024-12-15 20:00:00",
    "state_id": 3,  // 1=NS, 2=LIVE, 3=HT, 4=FT, 5=POSTPONED, etc.
    "scores": [
      {"description": "CURRENT", "score": {"home": 1, "away": 0}}
    ],
    "participants": [
      {"id": 101, "meta": {"location": "home"}},
      {"id": 102, "meta": {"location": "away"}}
    ],
    "events": [
      {"id": 789, "type": "goal", "minute": 32, "player_id": 5001, "team_id": 101},
      {"id": 790, "type": "yellow_card", "minute": 58, "player_id": 5002, "team_id": 102}
    ]
  }
}
```

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Provider API changes | Medium | High | Abstraction layer, version pinning |
| Rate limit exceeded | Medium | High | Exponential backoff, caching, webhook migration |
| Data inconsistency | Low | High | Validation, reconciliation job |
| Provider outage | Low | Critical | Graceful degradation, last-known-state caching |
| Cost escalation | Low | Medium | Monitor usage, alert at 80% quota |

## Next Steps

1. Create Sportmonks developer account
2. Subscribe to Starter plan (€29/mo) for 5 leagues
3. Obtain API token
4. Implement `SportmonksProvider` with official Java SDK
5. Write integration tests against live API
6. Configure webhook endpoint for push notifications (Phase 2)