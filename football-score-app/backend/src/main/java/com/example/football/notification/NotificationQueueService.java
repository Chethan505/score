package com.example.football.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamRecord;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationQueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${notification.queue.name:notification:queue}")
    private String queueName;

    @Value("${notification.queue.consumer-group:notification-workers}")
    private String consumerGroup;

    public NotificationQueueService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void enqueue(UUID notificationId) {
        Map<String, Object> message = Map.of(
                "notificationId", notificationId.toString(),
                "enqueuedAt", java.time.OffsetDateTime.now().toString()
        );

        redisTemplate.opsForStream().add(
                StreamRecord.create(message).withStreamKey(queueName)
        );
    }

    public void enqueueWithPriority(UUID notificationId, int priority) {
        Map<String, Object> message = Map.of(
                "notificationId", notificationId.toString(),
                "priority", String.valueOf(priority),
                "enqueuedAt", java.time.OffsetDateTime.now().toString()
        );

        redisTemplate.opsForStream().add(
                StreamRecord.create(message).withStreamKey(queueName + ":priority")
        );
    }

    public long getQueueSize() {
        return redisTemplate.opsForStream().size(queueName);
    }
}