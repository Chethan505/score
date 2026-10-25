# REA (Reverse Engineer Anything) Research

## Purpose
REA is a research toolkit for investigating existing football-score applications to understand:
- Product behavior patterns
- Feature implementations
- UX patterns worth adopting
- Architecture insights (where observable)

**NOT for:** Bypassing DRM, extracting proprietary code, unauthorized access, streaming

## REA Setup

### Installation
```bash
npx rea-agents@latest setup
```
- Registers REA's MCP server with the coding agent
- Installs matching workflow instructions
- Requires Node.js 22.x+/24.x+/26.12.1+ and npm
- Restart agent after setup

### Supported Targets for This Project
| Target Type | REA Capability | Use Case |
|-------------|----------------|----------|
| Websites | Browser observation, network capture | FotMob, SofaScore, Flashscore web |
| Android APKs | Manifest, classes, decompiled methods | FotMob APK, SofaScore APK |
| JavaScript/Electron | Modules, imports, IPC, routes | Web app internals |
| Network Captures (HAR) | Requests, responses, payloads | API traffic analysis |

## Reference Applications to Investigate

### 1. FotMob (Web & Android)
**Priority: High** - Popular, clean UX, good notification system

**Investigation Targets:**
- Competition organization (tabs vs dropdown)
- Live match indication (pulsing indicator, red "LIVE" badge)
- Match list grouping: LIVE / UPCOMING / FINISHED
- Star/favorite behavior (match vs team)
- Notification permission flow
- Event timeline (goals, cards, subs with minute markers)
- Match detail layout (score prominent, events chronological)
- Refresh behavior (pull-to-refresh, auto-refresh interval)
- Offline state handling

### 2. SofaScore (Web & Android)
**Priority: High** - Feature-rich, detailed statistics

**Investigation Targets:**
- League navigation (sidebar vs tabs)
- Live match "momentum" visualization
- Player ratings integration
- Match commentary/text timeline
- Favorite team vs favorite match distinction
- Push notification settings granularity
- Statistical widgets (possession, shots, xG)

### 3. Flashscore (Web)
**Priority: Medium** - Fast updates, minimal UI

**Investigation Targets:**
- Ultra-fast score updates (WebSocket?)
- Minimal match row design
- Audio/visual goal alerts
- Customizable notifications per match
- Archive/results organization

## REA Investigation Workflow

### For Web Applications (FotMob.com, SofaScore.com)
```bash
# 1. Start Chrome with debugging port
# Windows:
start chrome --remote-debugging-port=9222 --user-data-dir="C:\temp\chrome-debug"

# 2. Navigate to target site
# 3. Use REA browser observation
# Agent prompt: "Use REA to inspect https://www.fotmob.com. Analyze how live matches are displayed, how the star/favorite feature works, and how match events are structured in the timeline. Capture network requests during a live match."
```

### For Android APKs
```bash
# 1. Obtain APK (legitimate sources only - Google Play, official site)
# 2. Use REA Android analysis
# Agent prompt: "Use REA to analyze the FotMob APK. Find the notification permission flow, favorite match storage, and how live score updates are processed. Show the relevant classes and methods."
```

### For Network Analysis
```bash
# 1. Capture HAR file from browser DevTools
# 2. Use REA network capture analysis
# Agent prompt: "Use REA to analyze this HAR file from FotMob. Identify the API endpoints for fixtures, live scores, match events, and favorites. Show request/response structures."
```

## Expected Research Outputs

### 1. Match List Organization
```
FotMob:
- Top tabs: Live / Upcoming / Finished / Leagues
- Live matches: Red "LIVE" badge, pulsing animation
- Grouped by competition with collapsible headers
- Favorite matches pinned to top with star icon

SofaScore:
- Similar tab structure
- More prominent live match cards
- "Following" tab for favorites
```

### 2. Favorite/Star Behavior
```
FotMob:
- Star icon on each match row (match-level)
- Star icon on team in standings (team-level)
- "My Matches" / "My Teams" tabs
- Visual: Filled yellow star when active

SofaScore:
- Bell icon for notifications (per match)
- Heart icon for favorites (per team)
- Separate "Notifications" settings screen
```

### 3. Live Score Presentation
```
FotMob:
- Large score display
- Match minute (e.g., "45+2'")
- Red indicator for live
- Events: Goal (⚽), Yellow (🟨), Red (🟥), Sub (🔄)

SofaScore:
- Score + minute + momentum bar
- Detailed event timeline with player names
- xG timeline chart
```

### 4. Notification Concepts
```
FotMob:
- "Match Alerts" toggle per match
- Categories: Kick-off, Goals, Cards, HT/FT
- Managed in match detail screen

SofaScore:
- Granular per-match notification settings
- "Follow match" enables all notifications
- Quiet hours support
```

### 5. Network/Data Flow
```
FotMob API (observed):
- /api/leagues - competition list
- /api/matches?date=today&league=... - fixtures
- /api/matches/live - live matches (polled ~30s)
- /api/matches/{id} - match detail
- /api/matches/{id}/events - event timeline
- /api/user/favorites - user favorites
- WebSocket for real-time updates (wss://...)
```

## Key UX Patterns to Adopt

1. **Three-tab match organization**: LIVE / UPCOMING / FINISHED
2. **Prominent live indicator**: Red badge, pulsing, minute display
3. **Match-level favoriting**: Star on each row, not just team
4. **Event timeline**: Chronological with icons, minute markers
5. **Notification granularity**: Per-match toggle with categories
6. **Quick match access**: Favorite matches pinned/accessible
7. **Pull-to-refresh + auto-refresh**: Manual + background
8. **Clean match detail**: Score large, events scrollable

## Patterns to Avoid

1. **Cluttered UI**: Too many stats, ads, betting odds
2. **Mandatory account for basic features**: Allow guest browsing
3. **Aggressive notification prompts**: Ask after value shown
4. **Complex navigation**: Deep hierarchies for simple actions
5. **Slow initial load**: Prioritize critical data first

## REA Limitations & Ethics

### DO Use REA For:
- Observing public web behavior
- Analyzing APK structure (legitimately obtained)
- Understanding public API contracts (from network traffic)
- Learning UX patterns

### DO NOT Use REA For:
- Bypassing authentication/authorization
- Extracting private API keys
- Reverse engineering proprietary algorithms
- Accessing non-public endpoints
- Modifying/hacking applications
- Any copyright infringement

## Integration with Development

After REA research:
1. Document findings in `docs/rea-findings.md`
2. Create UI mockups based on patterns
3. Define API contracts inspired by observed patterns
4. Implement independent backend + frontend
5. **Never copy code** - only learn patterns

## Next Steps

1. Run `npx rea-agents@latest setup` to configure REA
2. Investigate FotMob web (primary reference)
3. Investigate SofaScore web (secondary)
4. Capture HAR files for API analysis
5. Document findings
6. Proceed to implementation