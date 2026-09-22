package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.application.dto.response.user.ScoutingCandidateCommand;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT new iuh.fit.auth.application.dto.response.user.ScoutingCandidateCommand(
                u.userId, u.username, u.fullName, u.avatarUrl,
                p.skillLevel, p.eloRating, p.matchCount, p.winRate, p.playRadiusKm,
                p.latitude, p.longitude)
            FROM PlayerSportProfileEntity p
            JOIN p.user u
            JOIN u.role r
            WHERE p.sportType = :sportType
              AND u.status = :status
              AND r.name = :roleName
              AND p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
              AND p.latitude BETWEEN :minLatitude AND :maxLatitude
              AND p.longitude BETWEEN :minLongitude AND :maxLongitude
            """)
    List<ScoutingCandidateCommand> findScoutingCandidates(
            @Param("sportType") SportType sportType,
            @Param("status") UserStatusEnum status,
            @Param("roleName") RoleEnum roleName,
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude,
            Pageable pageable
    );
}
