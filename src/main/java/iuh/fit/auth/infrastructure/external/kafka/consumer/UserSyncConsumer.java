package iuh.fit.auth.infrastructure.external.kafka.consumer;

import iuh.fit.auth.application.dto.event.UserRegistrationEvent;
import iuh.fit.auth.application.dto.event.UserRegistrationEventType;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "${spring.kafka.topic.user-sync}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeUserSyncEvent(UserRegistrationEvent event) {
        log.info("Received Kafka event: {} for user: {}", event.getEventType(), event.getData().getUsername());
        
        if (UserRegistrationEventType.USER_SYNCED.equals(event.getEventType())) {
            UUID userId = event.getData().getUserId();
            UUID keycloakId = event.getData().getKeycloakId();
            
            log.info("Syncing Keycloak ID: {} to local user ID: {}", keycloakId, userId);
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
                log.info("Successfully updated keycloakId for user: {}", user.getUsername());
            } else {
                log.warn("User with ID: {} not found in local DB. Cannot sync Keycloak ID.", userId);
            }
        }
    }
}
