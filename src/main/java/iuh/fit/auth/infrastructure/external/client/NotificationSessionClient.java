package iuh.fit.auth.infrastructure.external.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "notification-session", url = "${notification.service.url}", configuration = StorageClientConfig.class)
public interface NotificationSessionClient {
    @PostMapping("/internal/users/{userId}/session-revoked")
    void sessionRevoked(@PathVariable("userId") UUID userId, @RequestBody Map<String, String> body);
}
