package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserProviderAdapter implements CurrentUserProvider {
    private final SecurityContextHelper securityContextHelper;
    private final UserRepository userRepository;

    @Override
    public Optional<String> getCurrentUserId() {
        return securityContextHelper.getCurrentUserId();
    }

    @Override
    public Optional<String> getCurrentUserEmailOrUsername() {
        return securityContextHelper.getCurrentUser();
    }

    @Override
    public User getCurrentUser() throws ServiceException {
        String springUser = securityContextHelper.getCurrentUserId().orElse(null);

        if (springUser == null || springUser.isEmpty()) {
            throw new ServiceException("Phiên đăng nhập hết hạn");
        }

        User user;
        if (isUUID(springUser)) {
            user = userRepository.findById(UUID.fromString(springUser))
                    .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
        } else {
            user = userRepository.findByUsernameOrEmailWithRole(springUser, springUser)
                    .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
        }

        return user;
    }

    private boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
