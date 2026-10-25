export interface User {
  id: string
  phoneNumber: string
  whatsappOptIn: boolean
  timezone: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  userId: string
}

export interface Competition {
  id: string
  providerCompetitionId: string
  name: string
  country: string
  logoUrl: string
  currentSeasonId: string | null
}

export interface MatchSummary {
  id: string
  providerMatchId: string
  homeTeamId: string
  homeTeamName: string
  homeTeamShortName: string
  homeTeamLogoUrl: string
  awayTeamId: string
  awayTeamName: string
  awayTeamShortName: string
  awayTeamLogoUrl: string
  scheduledStart: string
  status: string
  homeScore: number
  awayScore: number
  minute: number | null
  extraMinute: number | null
}

export interface Team {
  id: string
  providerTeamId: string
  name: string
  shortName: string
  logoUrl: string
}

export interface MatchDetail {
  id: string
  providerMatchId: string
  competitionId: string
  competitionName: string
  homeTeam: Team
  awayTeam: Team
  scheduledStart: string
  status: string
  homeScore: number
  awayScore: number
  minute: number | null
  extraMinute: number | null
  events: MatchEvent[]
  homeLineup: Lineup[]
  awayLineup: Lineup[]
  statistics: Statistics | null
}

export interface MatchEvent {
  id: string
  providerEventId: string
  eventType: string
  team: Team | null
  playerId: string | null
  playerName: string | null
  minute: number | null
  extraMinute: number | null
  description: string | null
  metadata: string | null
  occurredAt: string
}

export interface Lineup {
  playerId: string
  playerName: string
  number: number | null
  position: string | null
  isStarter: boolean | null
  substitutedInMinute: number | null
  substitutedOutMinute: number | null
}

export interface Statistics {
  possessionHome: number | null
  possessionAway: number | null
  shotsHome: number | null
  shotsAway: number | null
  shotsOnTargetHome: number | null
  shotsOnTargetAway: number | null
  cornersHome: number | null
  cornersAway: number | null
  foulsHome: number | null
  foulsAway: number | null
  offsidesHome: number | null
  offsidesAway: number | null
  yellowCardsHome: number | null
  yellowCardsAway: number | null
  redCardsHome: number | null
  redCardsAway: number | null
}

export interface Notification {
  id: string
  matchId: string | null
  type: string
  templateName: string
  status: string
  scheduledFor: string | null
  sentAt: string | null
  deliveredAt: string | null
  createdAt: string
}

export interface NotificationPreferences {
  matchStartReminder: boolean
  goalNotifications: boolean
  cardNotifications: boolean
  substitutionNotifications: boolean
  halfTimeNotifications: boolean
  fullTimeNotifications: boolean
  reminderMinutesBefore: number
  quietHoursStart: string | null
  quietHoursEnd: string | null
}