package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.application.dto.response.user.UserSummaryResponseCommand;
import java.time.LocalDateTime;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByKeycloakId(UUID keycloakId);

    boolean existsByKeycloakId(UUID keycloakId);

    boolean existsByUsernameOrEmail(String username, String email);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.role WHERE u.username = :username OR u.email = :email")
    Optional<UserEntity> findByUsernameOrEmailWithRole(@Param("username") String username, @Param("email") String email);

    Optional<UserEntity> findByRole_Name(String name);

    @Query("""
            SELECT u FROM UserEntity u
            LEFT JOIN FETCH u.role r
            WHERE u.userId <> :excludedUserId
              AND u.status = :status
              AND r.name = :roleName
              AND (
                    LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY u.fullName ASC
            """)
    List<UserEntity> searchActivePlayers(
            @Param("keyword") String keyword,
            @Param("excludedUserId") UUID excludedUserId,
            @Param("status") UserStatusEnum status,
            @Param("roleName") String roleName,
            Pageable pageable
    );

    @Query("select role.name, count(u) from UserEntity u join u.role role group by role.name")
    List<Object[]> countGroupedByRole();

    @Query("select u.status, count(u) from UserEntity u group by u.status")
    List<Object[]> countGroupedByStatus();

    @Query("select u.createdAt from UserEntity u where u.createdAt >= :from and u.createdAt < :to")
    List<LocalDateTime> findCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    record UserReportRow(UUID userId, String fullName, String email, String role, UserStatusEnum status,
                         LocalDateTime createdAt) {}

    @Query("""
            select new iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository$UserReportRow(
                u.userId, u.fullName, u.email, r.name, u.status, u.createdAt)
            from UserEntity u join u.role r
            order by u.createdAt desc
            """)
    List<UserReportRow> findAllForReport();

    @Query("""
            select new iuh.fit.auth.application.dto.response.user.UserSummaryResponseCommand(u.userId, u.fullName, u.email)
            from UserEntity u where u.userId in :userIds
            """)
    List<UserSummaryResponseCommand> findSummariesByIds(@Param("userIds") Collection<UUID> userIds);

    @Query("""
            select u.userId from UserEntity u join u.role r
            where u.status = :status and r.name in :roleNames and u.userId > :after
            order by u.userId
            """)
    List<UUID> findIdsByStatusAndRoles(@Param("status") UserStatusEnum status,
                                       @Param("roleNames") Collection<String> roleNames,
                                       @Param("after") UUID after,
                                       Pageable pageable);

    @Query("select count(u) from UserEntity u join u.role r where u.status = :status and r.name in :roleNames")
    long countByStatusAndRoles(@Param("status") UserStatusEnum status,
                               @Param("roleNames") Collection<String> roleNames);
}
