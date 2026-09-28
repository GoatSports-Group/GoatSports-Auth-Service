package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.infrastructure.persistence.entity.AppliedMatchResultEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAppliedMatchResultRepository extends JpaRepository<AppliedMatchResultEntity, UUID> {
    Optional<AppliedMatchResultEntity> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    List<AppliedMatchResultEntity> findBySessionId(UUID sessionId);

    List<AppliedMatchResultEntity> findByUserIdAndSportTypeOrderByAppliedAtDesc(
            UUID userId, SportType sportType, Pageable pageable);
}
