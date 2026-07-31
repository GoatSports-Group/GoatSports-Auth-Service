package iuh.fit.auth.application.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationEvent {
    private UUID eventId;
    private UserRegistrationEventType eventType;
    private LocalDateTime timestamp;
    private UserRegistrationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegistrationData {
        private UUID userId;
        private UUID keycloakId;
        private String username;
        private String email;
        private String fullName;
        private String password;
        private boolean verified;
    }
}
