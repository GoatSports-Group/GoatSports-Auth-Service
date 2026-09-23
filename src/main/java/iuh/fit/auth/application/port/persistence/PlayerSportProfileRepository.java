package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.application.dto.response.user.ScoutedPlayerCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SportType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSportProfileRepository {
    List<PlayerSportProfile> findAllByUserId(UUID userId);

    List<ScoutedPlayerCommand> findScoutingCandidates(
            ScoutingSearchCommand search, double minLatitude, double maxLatitude,
            double minLongitude, double maxLongitude, int limit);

    List<ScoutedPlayerCommand> findScoutingProfiles(SportType sportType, Collection<UUID> userIds);

    Optional<PlayerSportProfile> findByIdAndUserId(UUID profileId, UUID userId);

    Optional<PlayerSportProfile> findByUserIdAndSportType(UUID userId, SportType sportType);

    boolean existsByUserIdAndSportType(UUID userId, SportType sportType);

    PlayerSportProfile save(PlayerSportProfile profile) throws ServiceException;

    void deleteByIdAndUserId(UUID profileId, UUID userId) throws ServiceException;
}
