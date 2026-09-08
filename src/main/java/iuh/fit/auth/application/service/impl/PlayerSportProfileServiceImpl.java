package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.SaveAvailabilityCommand;
import iuh.fit.auth.application.dto.request.user.SavePlayerSportProfileCommand;
import iuh.fit.auth.application.dto.response.user.AvailabilityResponseCommand;
import iuh.fit.auth.application.dto.response.user.PlayerSportProfileResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.service.PlayerSportProfileService;
import iuh.fit.auth.domain.entity.Availability;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.entity.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerSportProfileServiceImpl implements PlayerSportProfileService {
    private static final int DEFAULT_ELO_RATING = 1000;
    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    private final PlayerSportProfileRepository profileRepository;
    private final CurrentUserProvider currentUserProvider;

    public PlayerSportProfileServiceImpl(
            PlayerSportProfileRepository profileRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.profileRepository = profileRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<PlayerSportProfileResponseCommand> getMyProfiles() throws ServiceException {
        UUID userId = currentUserId();
        return profileRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PlayerSportProfileResponseCommand createMyProfile(
            SavePlayerSportProfileCommand command
    ) throws ServiceException {
        UUID userId = currentUserId();
        validateCommand(command);
        if (profileRepository.existsByUserIdAndSportType(userId, command.sportType())) {
            throw new ServiceException("Bạn đã có hồ sơ cho môn thể thao này");
        }

        PlayerSportProfile profile = PlayerSportProfile.builder()
                .userId(userId)
                .sportType(command.sportType())
                .skillLevel(command.skillLevel())
                .eloRating(DEFAULT_ELO_RATING)
                .preferredPositions(normalizePositions(command.preferredPositions()))
                .playStyle(normalizeOptional(command.playStyle()))
                .latitude(command.latitude())
                .longitude(command.longitude())
                .playRadiusKm(command.playRadiusKm())
                .winCount(0)
                .lossCount(0)
                .drawCount(0)
                .matchCount(0)
                .winRate(0.0)
                .availabilities(toAvailabilities(command.availabilities(), Set.of()))
                .build();

        return toResponse(profileRepository.save(profile));
    }

    @Override
    public PlayerSportProfileResponseCommand updateMyProfile(
            UUID profileId,
            SavePlayerSportProfileCommand command
    ) throws ServiceException {
        UUID userId = currentUserId();
        validateCommand(command);
        PlayerSportProfile profile = profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy hồ sơ thể thao"));

        if (profile.getSportType() != command.sportType()
                && profileRepository.existsByUserIdAndSportType(userId, command.sportType())) {
            throw new ServiceException("Bạn đã có hồ sơ cho môn thể thao này");
        }

        Set<UUID> existingAvailabilityIds = profile.getAvailabilities().stream()
                .map(Availability::getAvailabilityId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        profile.setSportType(command.sportType());
        profile.setSkillLevel(command.skillLevel());
        profile.setPreferredPositions(normalizePositions(command.preferredPositions()));
        profile.setPlayStyle(normalizeOptional(command.playStyle()));
        profile.setLatitude(command.latitude());
        profile.setLongitude(command.longitude());
        profile.setPlayRadiusKm(command.playRadiusKm());
        profile.setAvailabilities(toAvailabilities(command.availabilities(), existingAvailabilityIds));

        return toResponse(profileRepository.save(profile));
    }

    @Override
    public void deleteMyProfile(UUID profileId) throws ServiceException {
        profileRepository.deleteByIdAndUserId(profileId, currentUserId());
    }

    private UUID currentUserId() throws ServiceException {
        User currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getUserId() == null) {
            throw new ServiceException("Không tìm thấy người dùng hiện tại");
        }
        return currentUser.getUserId();
    }

    private void validateCommand(SavePlayerSportProfileCommand command) throws ServiceException {
        if (command == null || command.sportType() == null || command.skillLevel() == null) {
            throw new ServiceException("Môn thể thao và trình độ không được để trống");
        }
        if ((command.latitude() == null) != (command.longitude() == null)) {
            throw new ServiceException("Vĩ độ và kinh độ phải được cung cấp cùng nhau");
        }
        if (command.latitude() != null && (command.latitude() < -90 || command.latitude() > 90)) {
            throw new ServiceException("Vĩ độ phải nằm trong khoảng từ -90 đến 90");
        }
        if (command.longitude() != null && (command.longitude() < -180 || command.longitude() > 180)) {
            throw new ServiceException("Kinh độ phải nằm trong khoảng từ -180 đến 180");
        }
        if (command.playRadiusKm() != null
                && (command.playRadiusKm() <= 0 || command.playRadiusKm() > 200)) {
            throw new ServiceException("Bán kính chơi phải lớn hơn 0 và không vượt quá 200 km");
        }
        if (command.playStyle() != null && command.playStyle().trim().length() > 255) {
            throw new ServiceException("Phong cách chơi không được vượt quá 255 ký tự");
        }

        Set<String> positions = normalizePositions(command.preferredPositions());
        if (positions.size() > 10 || positions.stream().anyMatch(position -> position.length() > 50)) {
            throw new ServiceException("Tối đa 10 vị trí yêu thích, mỗi vị trí không quá 50 ký tự");
        }
        validateAvailabilities(command.availabilities());
    }

    private void validateAvailabilities(List<SaveAvailabilityCommand> commands) throws ServiceException {
        if (commands == null) return;

        for (SaveAvailabilityCommand command : commands) {
            if (command == null || command.dayOfWeek() == null
                    || command.startTime() == null || command.endTime() == null) {
                throw new ServiceException("Lịch rảnh phải có thứ, giờ bắt đầu và giờ kết thúc");
            }
            if (!command.startTime().isBefore(command.endTime())) {
                throw new ServiceException("Giờ bắt đầu phải sớm hơn giờ kết thúc");
            }
            String timezone = normalizeTimezone(command.timezone());
            if (timezone.length() > 100) {
                throw new ServiceException("Múi giờ không được vượt quá 100 ký tự");
            }
        }

        Map<Object, List<SaveAvailabilityCommand>> activeByDay = commands.stream()
                .filter(command -> command != null && !Boolean.FALSE.equals(command.active()))
                .collect(Collectors.groupingBy(SaveAvailabilityCommand::dayOfWeek));
        for (List<SaveAvailabilityCommand> dailySlots : activeByDay.values()) {
            List<SaveAvailabilityCommand> sorted = dailySlots.stream()
                    .sorted(Comparator.comparing(SaveAvailabilityCommand::startTime))
                    .toList();
            for (int index = 1; index < sorted.size(); index++) {
                if (sorted.get(index).startTime().isBefore(sorted.get(index - 1).endTime())) {
                    throw new ServiceException("Các khung giờ rảnh trong cùng một ngày không được chồng nhau");
                }
            }
        }
    }

    private Set<Availability> toAvailabilities(
            List<SaveAvailabilityCommand> commands,
            Set<UUID> existingIds
    ) throws ServiceException {
        if (commands == null) return new HashSet<>();

        Set<UUID> submittedIds = new HashSet<>();
        Set<Availability> result = new HashSet<>();
        for (SaveAvailabilityCommand command : commands) {
            UUID availabilityId = command.availabilityId();
            if (availabilityId != null && !existingIds.contains(availabilityId)) {
                throw new ServiceException("Lịch rảnh không thuộc hồ sơ thể thao hiện tại");
            }
            if (availabilityId != null && !submittedIds.add(availabilityId)) {
                throw new ServiceException("Lịch rảnh bị trùng lặp");
            }
            result.add(Availability.builder()
                    .availabilityId(availabilityId)
                    .dayOfWeek(command.dayOfWeek())
                    .startTime(command.startTime())
                    .endTime(command.endTime())
                    .timezone(normalizeTimezone(command.timezone()))
                    .active(!Boolean.FALSE.equals(command.active()))
                    .build());
        }
        return result;
    }

    private Set<String> normalizePositions(Set<String> positions) {
        if (positions == null) return new HashSet<>();
        return positions.stream()
                .filter(position -> position != null && !position.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeTimezone(String timezone) {
        return timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone.trim();
    }

    private PlayerSportProfileResponseCommand toResponse(PlayerSportProfile profile) {
        List<AvailabilityResponseCommand> availabilities = new ArrayList<>(profile.getAvailabilities()).stream()
                .sorted(Comparator.comparing(Availability::getDayOfWeek)
                        .thenComparing(Availability::getStartTime))
                .map(availability -> new AvailabilityResponseCommand(
                        availability.getAvailabilityId(),
                        availability.getDayOfWeek(),
                        availability.getStartTime(),
                        availability.getEndTime(),
                        availability.getTimezone(),
                        availability.getActive()
                ))
                .toList();
        return new PlayerSportProfileResponseCommand(
                profile.getProfileId(),
                profile.getSportType(),
                profile.getSkillLevel(),
                profile.getEloRating(),
                profile.getPreferredPositions(),
                profile.getPlayStyle(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getPlayRadiusKm(),
                profile.getWinCount(),
                profile.getLossCount(),
                profile.getDrawCount(),
                profile.getMatchCount(),
                profile.getWinRate(),
                availabilities,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
