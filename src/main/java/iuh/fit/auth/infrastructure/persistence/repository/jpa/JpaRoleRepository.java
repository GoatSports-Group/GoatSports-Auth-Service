package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID>, JpaSpecificationExecutor<RoleEntity> {
    Optional<RoleEntity> findByName(RoleEnum name);
}
