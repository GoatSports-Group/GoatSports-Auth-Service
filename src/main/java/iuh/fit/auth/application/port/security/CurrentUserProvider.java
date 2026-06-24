package iuh.fit.auth.application.port.security;

import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.exception.ServiceException;
import java.util.Optional;

public interface CurrentUserProvider {
    Optional<String> getCurrentUserId();

    Optional<String> getCurrentUserEmailOrUsername();

    User getCurrentUser() throws ServiceException;
}
