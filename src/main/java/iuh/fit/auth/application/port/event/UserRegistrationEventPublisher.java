package iuh.fit.auth.application.port.event;

import iuh.fit.auth.domain.entity.User;

public interface UserRegistrationEventPublisher {
    void publishUserRegistered(User user, String plainPassword);

    void publishUserVerified(User user);

    void publishUserPasswordUpdated(User user, String newPassword);

    void publishUserUpdated(User user);

    void publishUserDeleted(java.util.UUID userId);
}
