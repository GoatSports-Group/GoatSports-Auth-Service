package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.SaveAvailabilityCommand;
import iuh.fit.auth.application.dto.request.user.SavePlayerSportProfileCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerSportProfileServiceImplTest {
    @Mock
    private PlayerSportProfileRepository profileRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private PlayerSportProfileServiceImpl service;
    private UUID userId;

    @BeforeEach
    void setUp() throws ServiceException {
        service = new PlayerSportProfileServiceImpl(profileRepository, currentUserProvider);
        userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser()).thenReturn(User.builder().userId(userId).build());
    }

    @Test
    void createProfileAssignsSafeInitialMatchStatistics() throws ServiceException {
        SavePlayerSportProfileCommand command = validCommand(List.of(
                availability(null, DayOfWeek.MONDAY, "18:00", "20:00")
        ));
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(false);
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> {
            PlayerSportProfile profile = invocation.getArgument(0);
            profile.setProfileId(UUID.randomUUID());
            return profile;
        });

        var response = service.createMyProfile(command);

        assertEquals(1200, response.eloRating());
        assertEquals(0, response.matchCount());
        assertEquals(0.0, response.winRate());
        assertEquals("Asia/Ho_Chi_Minh", response.availabilities().getFirst().timezone());
        verify(profileRepository).save(any(PlayerSportProfile.class));
    }

    @Test
    void createProfileUsesDeclaredSkillForInitialElo() throws ServiceException {
        SavePlayerSportProfileCommand command = new SavePlayerSportProfileCommand(
                SportType.TENNIS,
                SkillLevel.PRO,
                Set.of(),
                "COMPETITIVE",
                10.762622,
                106.660172,
                15.0,
                List.of()
        );
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.TENNIS)).thenReturn(false);
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createMyProfile(command);

        assertEquals(1800, response.eloRating());
    }

    @Test
    void createProfileRejectsDuplicateSportForCurrentUser() throws ServiceException {
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(true);

        assertThrows(ServiceException.class, () -> service.createMyProfile(validCommand(List.of())));

        verify(profileRepository, never()).save(any(PlayerSportProfile.class));
    }

    @Test
    void createProfileRejectsOverlappingActiveAvailability() {
        SavePlayerSportProfileCommand command = validCommand(List.of(
                availability(null, DayOfWeek.SATURDAY, "08:00", "10:00"),
                availability(null, DayOfWeek.SATURDAY, "09:30", "11:00")
        ));

        assertThrows(ServiceException.class, () -> service.createMyProfile(command));
    }

    @Test
    void updateProfileRejectsAvailabilityFromAnotherProfile() throws ServiceException {
        UUID profileId = UUID.randomUUID();
        PlayerSportProfile existing = PlayerSportProfile.builder()
                .profileId(profileId)
                .userId(userId)
                .sportType(SportType.BADMINTON)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .availabilities(Set.of())
                .build();
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(java.util.Optional.of(existing));
        SavePlayerSportProfileCommand command = validCommand(List.of(
                availability(UUID.randomUUID(), DayOfWeek.MONDAY, "18:00", "20:00")
        ));

        assertThrows(ServiceException.class, () -> service.updateMyProfile(profileId, command));

        verify(profileRepository, never()).save(any(PlayerSportProfile.class));
    }

    private SavePlayerSportProfileCommand validCommand(List<SaveAvailabilityCommand> availabilities) {
        return new SavePlayerSportProfileCommand(
                SportType.BADMINTON,
                SkillLevel.INTERMEDIATE,
                Set.of("Đánh đôi"),
                "Kiểm soát cầu",
                10.762622,
                106.660172,
                15.0,
                availabilities
        );
    }

    private SaveAvailabilityCommand availability(
            UUID availabilityId,
            DayOfWeek day,
            String start,
            String end
    ) {
        return new SaveAvailabilityCommand(
                availabilityId,
                day,
                LocalTime.parse(start),
                LocalTime.parse(end),
                "",
                true
        );
    }
}
