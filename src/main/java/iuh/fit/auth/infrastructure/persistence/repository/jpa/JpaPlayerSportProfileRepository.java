package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
            SELECT p.profileId
            FROM PlayerSportProfileEntity p
            JOIN p.user u
            JOIN u.role r
            WHERE p.sportType = :sportType
              AND p.discoverable = true
              AND u.status = :status
              AND r.name = :roleName
              AND p.latitude BETWEEN :minLatitude AND :maxLatitude
              AND p.longitude BETWEEN :minLongitude AND :maxLongitude
              AND (:anySkill = true OR p.skillLevel IN :skillLevels)
              AND (:anyPosition = true OR EXISTS (
                    SELECT p2.profileId FROM PlayerSportProfileEntity p2 JOIN p2.preferredPositions pos
                    WHERE p2.profileId = p.profileId AND LOWER(pos) LIKE :positionPattern ESCAPE '\\'))
              AND (:anyDay = true OR EXISTS (
                    SELECT a.availabilityId FROM AvailabilityEntity a
                    WHERE a.profile = p AND a.active = true AND a.dayOfWeek = :day))
            ORDER BY (p.latitude - :latitude) * (p.latitude - :latitude)
                   + (p.longitude - :longitude) * (p.longitude - :longitude) * :longitudeWeight
            """)
    List<UUID> findScoutingProfileIds(
            @Param("sportType") SportType sportType,
            @Param("status") UserStatusEnum status,
            @Param("roleName") String roleName,
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("anySkill") boolean anySkill,
            @Param("skillLevels") Collection<SkillLevel> skillLevels,
            @Param("anyPosition") boolean anyPosition,
            @Param("positionPattern") String positionPattern,
            @Param("anyDay") boolean anyDay,
            @Param("day") DayOfWeek day,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("longitudeWeight") double longitudeWeight,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "preferredPositions", "availabilities"})
    List<PlayerSportProfileEntity> findAllByProfileIdIn(Collection<UUID> profileIds);

    @EntityGraph(attributePaths = {"user", "user.role", "preferredPositions", "availabilities"})
    List<PlayerSportProfileEntity> findAllBySportTypeAndUser_UserIdIn(SportType sportType, Collection<UUID> userIds);
}
