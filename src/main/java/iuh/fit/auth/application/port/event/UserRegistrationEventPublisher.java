package iuh.fit.auth.application.port.event;

import iuh.fit.auth.domain.entity.User;

import java.util.UUID;

public interface UserRegistrationEventPublisher {
    void publishUserRegistered(User user, String plainPassword);

    void publishUserVerified(User user);

    void publishUserPasswordUpdated(User user, String newPassword);

    void publishUserUpdated(User user);

    void publishUserDeleted(UUID userId);
}
