package iuh.fit.auth.application.port.persistence;

import java.time.LocalDateTime;
import java.util.Map;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.dto.response.base.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(UUID keycloakId);

    boolean existsByKeycloakId(UUID keycloakId);

    boolean existsByUsernameOrEmail(String username, String email);

    Optional<User> findByUsernameOrEmailWithRole(String username, String email);

    User save(User user) throws ServiceException;

    PageResult<User> findAll(PageFilter filter);

    List<User> findAllUsers();

    List<User> searchActivePlayers(String keyword, UUID excludedUserId, int limit);

    Optional<User> findByRole_Name(String name);

    /** Số tài khoản theo tên vai trò (PLAYER, VENUE_OWNER, ADMIN...). */
    Map<String, Long> countByRole();

    /** Số tài khoản theo trạng thái (ACTIVE, INACTIVE, PENDING, BLOCKED). */
    Map<String, Long> countByStatus();

    /** Thời điểm tạo của các tài khoản tạo trong [from, to). */
    List<LocalDateTime> findCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
