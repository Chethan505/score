package com.example.football.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndStatusIn(UUID userId, List<String> statuses);

    List<Notification> findByStatusAndScheduledForBefore(String status, OffsetDateTime now);

    List<Notification> findByMatchIdAndType(UUID matchId, String type);

    Optional<Notification> findByMatchIdAndEventIdAndType(UUID matchId, UUID eventId, String type);

    List<Notification> findByStatus(String status);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = :status, n.attemptCount = n.attemptCount + 1, n.lastError = :error WHERE n.id = :id")
    int incrementAttemptAndSetError(UUID id, String status, String error);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = 'SENT', n.sentAt = :now, n.whatsappMessageId = :msgId WHERE n.id = :id")
    int markAsSent(UUID id, OffsetDateTime now, String msgId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = 'DELIVERED', n.deliveredAt = :now WHERE n.id = :id")
    int markAsDelivered(UUID id, OffsetDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = 'DEAD_LETTER' WHERE n.id = :id")
    int markAsDeadLetter(UUID id);
}