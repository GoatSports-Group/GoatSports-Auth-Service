package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.domain.entity.Availability;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.infrastructure.persistence.entity.AvailabilityEntity;
import iuh.fit.auth.infrastructure.persistence.entity.PlayerSportProfileEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.PlayerSportProfileEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPlayerSportProfileRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        entity.setLatitude(profile.getLatitude());
        entity.setLongitude(profile.getLongitude());
        entity.setPlayRadiusKm(profile.getPlayRadiusKm());
        entity.setWinCount(profile.getWinCount());
        entity.setLossCount(profile.getLossCount());
        entity.setDrawCount(profile.getDrawCount());
        entity.setMatchCount(profile.getMatchCount());
        entity.setWinRate(profile.getWinRate());

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
