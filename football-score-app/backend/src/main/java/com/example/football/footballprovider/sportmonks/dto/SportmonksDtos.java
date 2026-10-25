package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksLeague(
        @JsonProperty("id") int id,
        @JsonProperty("name") String name,
        @JsonProperty("country_id") int countryId,
        @JsonProperty("country_name") String countryName,
        @JsonProperty("image_path") String imagePath,
        @JsonProperty("type") String type,
        @JsonProperty("season_id") Integer seasonId
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksSeason(
        @JsonProperty("id") int id,
        @JsonProperty("name") String name,
        @JsonProperty("starting_date") String startingDate,
        @JsonProperty("ending_date") String endingDate,
        @JsonProperty("is_current") boolean isCurrent
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksTeam(
        @JsonProperty("id") int id,
        @JsonProperty("name") String name,
        @JsonProperty("short_code") String shortCode,
        @JsonProperty("image_path") String imagePath,
        @JsonProperty("country_id") int countryId,
        @JsonProperty("venue_id") Integer venueId
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksFixture(
        @JsonProperty("id") long id,
        @JsonProperty("league_id") int leagueId,
        @JsonProperty("season_id") int seasonId,
        @JsonProperty("stage_id") int stageId,
        @JsonProperty("round_id") Integer roundId,
        @JsonProperty("group_id") Integer groupId,
        @JsonProperty("starting_at") OffsetDateTime startingAt,
        @JsonProperty("result_info") String resultInfo,
        @JsonProperty("scores") List<SportmonksScore> scores,
        @JsonProperty("time") SportmonksTime time,
        @JsonProperty("state_id") int stateId,
        @JsonProperty("participants") List<SportmonksParticipant> participants,
        @JsonProperty("venue_id") Integer venueId,
        @JsonProperty("details") String details,
        @JsonProperty("note") String note,
        @JsonProperty("periods") List<Object> periods,
        @JsonProperty("deleted") boolean deleted,
        @JsonProperty("events") List<SportmonksEvent> events,
        @JsonProperty("lineups") SportmonksLineups lineups,
        @JsonProperty("statistics") SportmonksStatistics statistics
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksScore(
        @JsonProperty("description") String description,
        @JsonProperty("score") SportmonksScoreDetail score
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksScoreDetail(
        @JsonProperty("goals") Integer goals,
        @JsonProperty("penalty_shootout") Integer penaltyShootout
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksTime(
        @JsonProperty("minute") Integer minute,
        @JsonProperty("added_time") Integer addedTime,
        @JsonProperty("extra_minute") Integer extraMinute,
        @JsonProperty("injury_time") Integer injuryTime
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksParticipant(
        @JsonProperty("id") int id,
        @JsonProperty("name") String name,
        @JsonProperty("image_path") String imagePath,
        @JsonProperty("meta") SportmonksParticipantMeta meta
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksParticipantMeta(
        @JsonProperty("location") String location,
        @JsonProperty("winner") Boolean winner
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksEvent(
        @JsonProperty("id") long id,
        @JsonProperty("fixture_id") long fixtureId,
        @JsonProperty("type_id") int typeId,
        @JsonProperty("type") String type,
        @JsonProperty("minute") Integer minute,
        @JsonProperty("extra_minute") Integer extraMinute,
        @JsonProperty("injury_time") Boolean injuryTime,
        @JsonProperty("team_id") int teamId,
        @JsonProperty("player_id") Integer playerId,
        @JsonProperty("player_name") String playerName,
        @JsonProperty("assist_player_id") Integer assistPlayerId,
        @JsonProperty("description") String description,
        @JsonProperty("information") List<String> information
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksLineups(
        @JsonProperty("home") List<SportmonksLineupPlayer> home,
        @JsonProperty("away") List<SportmonksLineupPlayer> away
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksLineupPlayer(
        @JsonProperty("player_id") int playerId,
        @JsonProperty("player_name") String playerName,
        @JsonProperty("jersey_number") int jerseyNumber,
        @JsonProperty("position") String position,
        @JsonProperty("is_starter") boolean isStarter,
        @JsonProperty("substituted_in") Integer substitutedIn,
        @JsonProperty("substituted_out") Integer substitutedOut
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksStatistics(
        @JsonProperty("home") List<SportmonksStatistic> home,
        @JsonProperty("away") List<SportmonksStatistic> away
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksStatistic(
        @JsonProperty("type_id") int typeId,
        @JsonProperty("type") String type,
        @JsonProperty("value") Object value
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksStanding(
        @JsonProperty("team_id") int teamId,
        @JsonProperty("position") int position,
        @JsonProperty("overall") SportmonksStandingDetail overall,
        @JsonProperty("home") SportmonksStandingDetail home,
        @JsonProperty("away") SportmonksStandingDetail away
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksStandingDetail(
        @JsonProperty("played") int played,
        @JsonProperty("won") int won,
        @JsonProperty("draw") int draw,
        @JsonProperty("lost") int lost,
        @JsonProperty("goals_for") int goalsFor,
        @JsonProperty("goals_against") int goalsAgainst,
        @JsonProperty("goal_difference") int goalDifference,
        @JsonProperty("points") int points,
        @JsonProperty("form") String form
) {}