package iuh.fit.auth.infrastructure.external.adapter;

import iuh.fit.auth.application.port.client.SessionRevocationPort;
import iuh.fit.auth.infrastructure.external.client.NotificationSessionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionRevocationAdapter implements SessionRevocationPort {
    private final NotificationSessionClient client;

    @Override
    public void notifySessionRevoked(UUID userId, String reason) {
        try {
            client.sessionRevoked(userId, Map.of("reason", reason));
        } catch (Exception e) {
            log.warn("Could not push session revocation to user {} ({}): {}", userId, reason, e.getMessage());
        }
    }
}
