package iuh.fit.auth.infrastructure.external.kafka.producer;

import iuh.fit.auth.application.dto.event.UserRegistrationEvent;
import iuh.fit.auth.application.dto.event.UserRegistrationEventType;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationEventPublisherAdapter implements UserRegistrationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.user-registration}")
    private String topic;

    @Override
    public void publishUserRegistered(User user, String plainPassword) {
        UserRegistrationEvent event = toUserRegistrationEvent(user, plainPassword);
        try {
            kafkaTemplate.send(topic, user.getUserId().toString(), event).get();
            log.info("Sent user-registration event to Kafka successfully for user ID: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to send user-registration event to Kafka for user ID: {}", user.getUserId(), e);
            throw new RuntimeException("Keycloak sync event publication failed, rolling back transaction", e);
        }
    }

    @Override
    public void publishUserVerified(User user) {
        UserRegistrationEvent event = UserRegistrationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(UserRegistrationEventType.USER_VERIFIED)
                .timestamp(LocalDateTime.now())
                .data(UserRegistrationEvent.UserRegistrationData.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build())
                .build();
        try {
            kafkaTemplate.send(topic, user.getUserId().toString(), event).get();
            log.info("Sent user-verification event to Kafka successfully for user ID: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to send user-verification event to Kafka for user ID: {}", user.getUserId(), e);
            throw new RuntimeException("User verification event publication failed, rolling back transaction", e);
        }
    }

    @Override
    public void publishUserPasswordUpdated(User user, String newPassword) {
        UserRegistrationEvent event = UserRegistrationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(UserRegistrationEventType.USER_PASSWORD_UPDATED)
                .timestamp(LocalDateTime.now())
                .data(UserRegistrationEvent.UserRegistrationData.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .password(newPassword)
                        .build())
                .build();
        try {
            kafkaTemplate.send(topic, user.getUserId().toString(), event).get();
            log.info("Sent user-password-updated event to Kafka successfully for user ID: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to send user-password-updated event to Kafka for user ID: {}", user.getUserId(), e);
            throw new RuntimeException("User password-update event publication failed, rolling back transaction", e);
        }
    }

    @Override
    public void publishUserUpdated(User user) {
        UserRegistrationEvent event = UserRegistrationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(UserRegistrationEventType.USER_UPDATED)
                .timestamp(LocalDateTime.now())
                .data(UserRegistrationEvent.UserRegistrationData.builder()
                        .userId(user.getUserId())
                        .keycloakId(user.getKeycloakId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build())
                .build();
        try {
            kafkaTemplate.send(topic, user.getUserId().toString(), event).get();
            log.info("Sent user-updated event to Kafka successfully for user ID: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to send user-updated event to Kafka for user ID: {}", user.getUserId(), e);
            throw new RuntimeException("User update event publication failed, rolling back transaction", e);
        }
    }

    private UserRegistrationEvent toUserRegistrationEvent(User user, String plainPassword) {
        return UserRegistrationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(UserRegistrationEventType.USER_REGISTERED)
                .timestamp(LocalDateTime.now())
                .data(UserRegistrationEvent.UserRegistrationData.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .password(plainPassword)
                        .build()
                )
                .build();
    }
}
