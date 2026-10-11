package iuh.fit.auth.application.port.persistence;

import java.time.LocalDateTime;
import java.util.Map;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.dto.response.base.PageResult;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserSummaryResponseCommand;

import java.util.Collection;
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

    List<UserReportResponseCommand> findAllForReport();

    List<UserSummaryResponseCommand> findSummariesByIds(Collection<UUID> userIds);

    List<UUID> findActiveIdsByRoles(Collection<String> roleNames, UUID after, int limit);

    long countActiveByRoles(Collection<String> roleNames);

    List<User> searchActivePlayers(String keyword, UUID excludedUserId, int limit);

    Optional<User> findByRole_Name(String name);

    Map<String, Long> countByRole();

    Map<String, Long> countByStatus();

    List<LocalDateTime> findCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
