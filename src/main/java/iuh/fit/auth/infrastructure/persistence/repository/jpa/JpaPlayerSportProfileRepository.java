package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPlayerSportProfileRepository extends JpaRepository<PlayerSportProfileEntity, UUID> {
    @EntityGraph(attributePaths = {"preferredPositions", "availabilities"})
    List<PlayerSportProfileEntity> findAllByUser_UserIdOrderByCreatedAtAsc(UUID userId);

    @EntityGraph(attributePaths = {"preferredPositions", "availabilities"})
    Optional<PlayerSportProfileEntity> findByProfileIdAndUser_UserId(UUID profileId, UUID userId);

    @EntityGraph(attributePaths = {"preferredPositions", "availabilities"})
    Optional<PlayerSportProfileEntity> findByUser_UserIdAndSportType(UUID userId, SportType sportType);

    boolean existsByUser_UserIdAndSportType(UUID userId, SportType sportType);
}
