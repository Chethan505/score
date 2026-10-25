-- Football Score App - Initial Schema
-- V1__initial_schema.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    whatsapp_opt_in BOOLEAN DEFAULT FALSE,
    opt_in_at TIMESTAMPTZ,
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Competitions table (reference data)
CREATE TABLE competitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_competition_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    logo_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Teams table
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_team_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    short_name VARCHAR(20),
    logo_url VARCHAR(500),
    competition_id UUID REFERENCES competitions(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seasons table
CREATE TABLE seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_season_id VARCHAR(50) UNIQUE NOT NULL,
    competition_id UUID NOT NULL REFERENCES competitions(id),
    name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Matches table (core entity)
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_match_id VARCHAR(50) UNIQUE NOT NULL,
    competition_id UUID NOT NULL REFERENCES competitions(id),
    season_id UUID REFERENCES seasons(id),
    home_team_id UUID NOT NULL REFERENCES teams(id),
    away_team_id UUID NOT NULL REFERENCES teams(id),
    scheduled_start TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    home_score INTEGER DEFAULT 0,
    away_score INTEGER DEFAULT 0,
    match_minute INTEGER,
    extra_minute INTEGER,
    last_provider_update TIMESTAMPTZ,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Match Events table
CREATE TABLE match_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    provider_event_id VARCHAR(50),
    event_type VARCHAR(30) NOT NULL,
    team_id UUID REFERENCES teams(id),
    player_id UUID,
    player_name VARCHAR(100),
    minute INTEGER,
    extra_minute INTEGER,
    description TEXT,
    metadata JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(match_id, provider_event_id)
);

-- Standings table
CREATE TABLE standings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    competition_id UUID NOT NULL REFERENCES competitions(id),
    season_id UUID NOT NULL REFERENCES seasons(id),
    team_id UUID NOT NULL REFERENCES teams(id),
    position INTEGER NOT NULL,
    played INTEGER DEFAULT 0,
    won INTEGER DEFAULT 0,
    draw INTEGER DEFAULT 0,
    lost INTEGER DEFAULT 0,
    goals_for INTEGER DEFAULT 0,
    goals_against INTEGER DEFAULT 0,
    goal_difference INTEGER DEFAULT 0,
    points INTEGER DEFAULT 0,
    form VARCHAR(20),
    last_updated TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(competition_id, season_id, team_id)
);

-- Favorite Matches (user-match)
CREATE TABLE favorite_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, match_id)
);

-- Favorite Teams (user-team)
CREATE TABLE favorite_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, team_id)
);

-- Notification Preferences
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_start_reminder BOOLEAN DEFAULT TRUE,
    goal_notifications BOOLEAN DEFAULT TRUE,
    card_notifications BOOLEAN DEFAULT FALSE,
    substitution_notifications BOOLEAN DEFAULT FALSE,
    half_time_notifications BOOLEAN DEFAULT FALSE,
    full_time_notifications BOOLEAN DEFAULT TRUE,
    reminder_minutes_before INTEGER DEFAULT 15,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Notifications (audit log)
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE SET NULL,
    event_id UUID REFERENCES match_events(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    whatsapp_message_id VARCHAR(128),
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
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    response_body JSONB,
    attempted_at TIMESTAMPTZ DEFAULT NOW()
);

-- ShedLock table for distributed scheduling
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Indexes for performance
CREATE INDEX idx_matches_status_scheduled ON matches(status, scheduled_start);
CREATE INDEX idx_matches_live ON matches(status) WHERE status IN ('LIVE', 'HALFTIME');
CREATE INDEX idx_matches_competition_date ON matches(competition_id, scheduled_start);
CREATE INDEX idx_matches_teams ON matches(home_team_id, away_team_id);
CREATE INDEX idx_match_events_match ON match_events(match_id, occurred_at);
CREATE INDEX idx_match_events_type ON match_events(event_type);
CREATE INDEX idx_favorite_matches_user ON favorite_matches(user_id);
CREATE INDEX idx_favorite_teams_user ON favorite_teams(user_id);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_for, status) WHERE status = 'PENDING';
CREATE INDEX idx_notification_attempts_notification ON notification_attempts(notification_id);
CREATE INDEX idx_standings_competition_season ON standings(competition_id, season_id, position);