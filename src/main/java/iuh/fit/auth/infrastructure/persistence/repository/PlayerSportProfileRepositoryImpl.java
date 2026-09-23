package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.domain.entity.Availability;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.application.dto.response.user.AvailabilityResponseCommand;
import iuh.fit.auth.application.dto.response.user.ScoutedPlayerCommand;
import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.AvailabilityEntity;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.PlayerSportProfileEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPlayerSportProfileRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlayerSportProfileRepositoryImpl implements PlayerSportProfileRepository {
    private final JpaPlayerSportProfileRepository jpaProfileRepository;
    private final JpaUserRepository jpaUserRepository;
    private final PlayerSportProfileEntityMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScoutedPlayerCommand> findScoutingCandidates(
            ScoutingSearchCommand search, double minLatitude, double maxLatitude,
            double minLongitude, double maxLongitude, int limit) {
        double longitudeScale = Math.cos(Math.toRadians(search.latitude()));
        List<UUID> ids = jpaProfileRepository.findScoutingProfileIds(
                search.sportType(), UserStatusEnum.ACTIVE, RoleEnum.PLAYER,
                minLatitude, maxLatitude, minLongitude, maxLongitude,
                search.anySkill(), search.anySkill() ? EnumSet.allOf(SkillLevel.class) : search.skillLevels(),
                search.anyPosition(), search.anyPosition() ? "" : "%" + escapeLike(search.position().trim().toLowerCase(Locale.ROOT)) + "%",
                search.availableOn() == null, search.availableOn() == null ? DayOfWeek.MONDAY : search.availableOn(),
                search.latitude(), search.longitude(), longitudeScale * longitudeScale,
                PageRequest.of(0, limit));
        if (ids.isEmpty()) return List.of();

        // Keep the nearest-first order of the id query.
        Map<UUID, PlayerSportProfileEntity> byId = new HashMap<>();
        jpaProfileRepository.findAllByProfileIdIn(ids).forEach(entity -> byId.put(entity.getProfileId(), entity));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(this::toScouted).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoutedPlayerCommand> findScoutingProfiles(SportType sportType, Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        Map<UUID, PlayerSportProfileEntity> profiles = new HashMap<>();
        jpaProfileRepository.findAllBySportTypeAndUser_UserIdIn(sportType, userIds)
                .forEach(entity -> profiles.put(entity.getUser().getUserId(), entity));

        return jpaUserRepository.findAllById(userIds).stream()
                .map(user -> {
                    PlayerSportProfileEntity profile = profiles.get(user.getUserId());
                    return profile != null && visibleToScouts(profile)
                            ? toScouted(profile)
                            : ScoutedPlayerCommand.identityOnly(
                                    user.getUserId(), user.getUsername(), user.getFullName(), user.getAvatarUrl());
                })
                .toList();
    }

    /** The position filter is typed by a person: '%' and '_' are literal characters, not wildcards. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean visibleToScouts(PlayerSportProfileEntity profile) {
        return !Boolean.FALSE.equals(profile.getDiscoverable())
                && profile.getUser().getStatus() == UserStatusEnum.ACTIVE;
    }

    private ScoutedPlayerCommand toScouted(PlayerSportProfileEntity profile) {
        UserEntity user = profile.getUser();
        List<AvailabilityResponseCommand> availabilities = profile.getAvailabilities().stream()
                .filter(slot -> !Boolean.FALSE.equals(slot.getActive()))
                .sorted(Comparator.comparing(AvailabilityEntity::getDayOfWeek)
                        .thenComparing(AvailabilityEntity::getStartTime))
                .map(slot -> new AvailabilityResponseCommand(slot.getAvailabilityId(), slot.getDayOfWeek(),
                        slot.getStartTime(), slot.getEndTime(), slot.getTimezone(), slot.getActive()))
                .toList();
        return new ScoutedPlayerCommand(
                user.getUserId(), user.getUsername(), user.getFullName(), user.getAvatarUrl(), true,
                profile.getSkillLevel(), profile.getEloRating(), Set.copyOf(profile.getPreferredPositions()),
                profile.getPlayStyle(), profile.getCity(), profile.getPlayRadiusKm(),
                profile.getWinCount(), profile.getLossCount(), profile.getDrawCount(), profile.getMatchCount(),
                profile.getWinRate(), availabilities, profile.getLatitude(), profile.getLongitude(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSportProfile> findAllByUserId(UUID userId) {
        return jpaProfileRepository.findAllByUser_UserIdOrderByCreatedAtAsc(userId).stream()
                .map(profileMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerSportProfile> findByIdAndUserId(UUID profileId, UUID userId) {
        return jpaProfileRepository.findByProfileIdAndUser_UserId(profileId, userId)
                .map(profileMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerSportProfile> findByUserIdAndSportType(UUID userId, SportType sportType) {
        return jpaProfileRepository.findByUser_UserIdAndSportType(userId, sportType)
                .map(profileMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndSportType(UUID userId, SportType sportType) {
        return jpaProfileRepository.existsByUser_UserIdAndSportType(userId, sportType);
    }

    @Override
    @Transactional
    public PlayerSportProfile save(PlayerSportProfile profile) throws ServiceException {
        if (profile.getUserId() == null) {
            throw new ServiceException("Hồ sơ thể thao phải thuộc một người dùng");
        }

        PlayerSportProfileEntity entity;
        if (profile.getProfileId() == null) {
            entity = new PlayerSportProfileEntity();
            entity.setUser(jpaUserRepository.findById(profile.getUserId())
                    .orElseThrow(() -> new ServiceException("Người dùng không tồn tại")));
            entity.setPreferredPositions(new HashSet<>());
            entity.setAvailabilities(new HashSet<>());
        } else {
            entity = jpaProfileRepository.findByProfileIdAndUser_UserId(
                            profile.getProfileId(),
                            profile.getUserId()
                    )
                    .orElseThrow(() -> new ServiceException("Không tìm thấy hồ sơ thể thao"));
        }

        updateProfile(entity, profile);
        PlayerSportProfileEntity saved = jpaProfileRepository.save(entity);
        return profileMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(UUID profileId, UUID userId) throws ServiceException {
        PlayerSportProfileEntity entity = jpaProfileRepository.findByProfileIdAndUser_UserId(profileId, userId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy hồ sơ thể thao"));
        jpaProfileRepository.delete(entity);
    }

    private void updateProfile(PlayerSportProfileEntity entity, PlayerSportProfile profile) {
        entity.setSportType(profile.getSportType());
        entity.setSkillLevel(profile.getSkillLevel());
        entity.setEloRating(profile.getEloRating());
        entity.setPlayStyle(profile.getPlayStyle());
        entity.setCity(profile.getCity());
        entity.setLatitude(profile.getLatitude());
        entity.setLongitude(profile.getLongitude());
        entity.setPlayRadiusKm(profile.getPlayRadiusKm());
        entity.setWinCount(profile.getWinCount());
        entity.setLossCount(profile.getLossCount());
        entity.setDrawCount(profile.getDrawCount());
        entity.setMatchCount(profile.getMatchCount());
        entity.setWinRate(profile.getWinRate());
        entity.setDiscoverable(!Boolean.FALSE.equals(profile.getDiscoverable()));

        entity.getPreferredPositions().clear();
        entity.getPreferredPositions().addAll(profile.getPreferredPositions());
        synchronizeAvailabilities(entity, profile.getAvailabilities());
    }

    private void synchronizeAvailabilities(
            PlayerSportProfileEntity profile,
            Set<Availability> requestedAvailabilities
    ) {
        Map<UUID, AvailabilityEntity> existingById = new HashMap<>();
        for (AvailabilityEntity entity : profile.getAvailabilities()) {
            if (entity.getAvailabilityId() != null) {
                existingById.put(entity.getAvailabilityId(), entity);
            }
        }

        Set<UUID> retainedIds = new HashSet<>();
        for (Availability requested : requestedAvailabilities) {
            AvailabilityEntity entity = requested.getAvailabilityId() == null
                    ? new AvailabilityEntity()
                    : existingById.get(requested.getAvailabilityId());
            if (entity == null) continue;

            entity.setProfile(profile);
            entity.setDayOfWeek(requested.getDayOfWeek());
            entity.setStartTime(requested.getStartTime());
            entity.setEndTime(requested.getEndTime());
            entity.setTimezone(requested.getTimezone());
            entity.setActive(requested.getActive());
            if (requested.getAvailabilityId() == null) {
                profile.getAvailabilities().add(entity);
            } else {
                retainedIds.add(requested.getAvailabilityId());
            }
        }

        profile.getAvailabilities().removeIf(entity -> entity.getAvailabilityId() != null
                && !retainedIds.contains(entity.getAvailabilityId()));
    }
}
