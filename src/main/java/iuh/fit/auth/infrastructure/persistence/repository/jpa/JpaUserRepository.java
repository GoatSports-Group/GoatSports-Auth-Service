package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Optional<UserEntity> findByRole_Name(RoleEnum name);

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
            @Param("status") iuh.fit.auth.domain.enums.UserStatusEnum status,
            @Param("roleName") RoleEnum roleName,
            Pageable pageable
    );
}
