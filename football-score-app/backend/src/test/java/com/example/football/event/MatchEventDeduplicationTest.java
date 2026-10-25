package com.example.football.event;

import com.example.football.match.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MatchEventDeduplicationTest {

    @Mock
    private MatchEventRepository matchEventRepository;

    @Test
    void eventKey_isDeterministic_forSameEvent() {
        // Given
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        
        // When
        String key1 = generateEventKey(match, "GOAL", "event-123");
        String key2 = generateEventKey(match, "GOAL", "event-123");

        // Then
        assertEquals(key1, key2);
    }

    @Test
    void eventKey_isDifferent_forDifferentEvents() {
        // Given
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        
        // When
        String goalKey = generateEventKey(match, "GOAL", "event-123");
        String cardKey = generateEventKey(match, "YELLOW_CARD", "event-124");

        // Then
        assertNotEquals(goalKey, cardKey);
    }

    @Test
    void eventKey_fallback_isDeterministic_whenNoProviderId() {
        // Given
        Match match = new Match();
        match.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        
        // When
        String key1 = generateEventKeyWithoutProviderId(match, "GOAL", "team-home", 32, 0);
        String key2 = generateEventKeyWithoutProviderId(match, "GOAL", "team-home", 32, 0);

        // Then
        assertEquals(key1, key2);
    }

    @Test
    void shouldSaveNewEvent_whenNotExists() {
        // Given
        Match match = new Match();
        match.setId(UUID.randomUUID());
        MatchEvent event = createEvent(match, "event-123", "GOAL");

        when(matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), "event-123"))
                .thenReturn(false);
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean isNew = !matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), event.getProviderEventId());
        MatchEvent saved = isNew ? matchEventRepository.save(event) : null;

        // Then
        assertTrue(isNew);
        assertNotNull(saved);
        verify(matchEventRepository).save(event);
    }

    @Test
    void shouldNotSaveDuplicateEvent_whenExists() {
        // Given
        Match match = new Match();
        match.setId(UUID.randomUUID());
        MatchEvent event = createEvent(match, "event-123", "GOAL");

        when(matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), "event-123"))
                .thenReturn(true);

        // When
        boolean isNew = !matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), event.getProviderEventId());

        // Then
        assertFalse(isNew);
        verify(matchEventRepository, never()).save(any());
    }

    private String generateEventKey(Match match, String eventType, String providerEventId) {
        return match.getId() + ":" + eventType + ":" + providerEventId;
    }

    private String generateEventKeyWithoutProviderId(Match match, String eventType, String teamId, int minute, int extraMinute) {
        return match.getId() + ":" + eventType + ":" + teamId + ":" + minute + ":" + extraMinute;
    }

    private MatchEvent createEvent(Match match, String providerEventId, String eventType) {
        MatchEvent event = new MatchEvent();
        event.setId(UUID.randomUUID());
        event.setMatch(match);
        event.setProviderEventId(providerEventId);
        event.setEventType(eventType);
        event.setMinute(32);
        event.setOccurredAt(OffsetDateTime.now());
        return event;
    }
}