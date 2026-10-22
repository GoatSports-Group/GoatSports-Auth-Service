package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.SaveAvailabilityCommand;
import iuh.fit.auth.application.dto.request.user.SavePlayerSportProfileCommand;
import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.application.dto.response.user.PlayerSportProfileResponseCommand;
import iuh.fit.auth.application.dto.response.user.ScoutedPlayerCommand;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
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
        // Scouting tests never read the current user.
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(User.builder().userId(userId).build());
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

        PlayerSportProfileResponseCommand response = service.createMyProfile(command);

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
                "Hồ Chí Minh",
                10.762622,
                106.660172,
                15.0,
                List.of(),
                null
        );
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.TENNIS)).thenReturn(false);
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerSportProfileResponseCommand response = service.createMyProfile(command);

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
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(existing));
        SavePlayerSportProfileCommand command = validCommand(List.of(
                availability(UUID.randomUUID(), DayOfWeek.MONDAY, "18:00", "20:00")
        ));

        assertThrows(ServiceException.class, () -> service.updateMyProfile(profileId, command));

        verify(profileRepository, never()).save(any(PlayerSportProfile.class));
    }

    @Test
    void createProfileStoresTrimmedCity() throws ServiceException {
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(false);
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SavePlayerSportProfileCommand command = new SavePlayerSportProfileCommand(
                SportType.BADMINTON, SkillLevel.INTERMEDIATE, Set.of(), null,
                "  Hà Nội  ", 21.0285, 105.8542, 10.0, List.of(), null);

        PlayerSportProfileResponseCommand response = service.createMyProfile(command);

        assertEquals("Hà Nội", response.city());
    }

    @Test
    void createProfileRejectsMissingCity() throws ServiceException {
        SavePlayerSportProfileCommand command = new SavePlayerSportProfileCommand(
                SportType.BADMINTON, SkillLevel.INTERMEDIATE, Set.of(), null,
                " ", 21.0285, 105.8542, 10.0, List.of(), null);

        assertThrows(ServiceException.class, () -> service.createMyProfile(command));

        verify(profileRepository, never()).save(any(PlayerSportProfile.class));
    }

    @Test
    void updateProfileRejectsChangingSport() throws ServiceException {
        UUID profileId = UUID.randomUUID();
        PlayerSportProfile existing = PlayerSportProfile.builder()
                .profileId(profileId)
                .userId(userId)
                .sportType(SportType.TENNIS)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .availabilities(Set.of())
                .build();
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(existing));

        assertThrows(ServiceException.class, () -> service.updateMyProfile(profileId, validCommand(List.of())));

        verify(profileRepository, never()).save(any(PlayerSportProfile.class));
    }

    @Test
    void newProfilesAreDiscoverableUnlessThePlayerOptsOut() throws ServiceException {
        when(profileRepository.existsByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(false);
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(true, service.createMyProfile(validCommand(List.of())).discoverable());
        assertEquals(false, service.createMyProfile(withDiscoverable(validCommand(List.of()), false)).discoverable());
    }

    @Test
    void updateKeepsScoutingVisibilityWhenNotSent() throws ServiceException {
        UUID profileId = UUID.randomUUID();
        PlayerSportProfile existing = PlayerSportProfile.builder()
                .profileId(profileId).userId(userId).sportType(SportType.BADMINTON)
                .skillLevel(SkillLevel.INTERMEDIATE).matchCount(0).discoverable(false)
                .availabilities(new HashSet<>())
                .build();
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(PlayerSportProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(false, service.updateMyProfile(profileId, validCommand(List.of())).discoverable());
        assertEquals(true, service.updateMyProfile(profileId, withDiscoverable(validCommand(List.of()), true)).discoverable());
    }

    @Test
    void scoutingDropsBoxCornersOutsideTheRadiusAndSortsByDistance() {
        ScoutingSearchCommand search = new ScoutingSearchCommand(
                SportType.FOOTBALL, 21.0285, 105.8542, 10, 5, null, null, null);
        when(profileRepository.findScoutingCandidates(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(
                        scouted("corner", 21.0285 + 0.085, 105.8542 + 0.09), // inside the box, ~13 km away
                        scouted("close", 21.0285 + 0.02, 105.8542),
                        scouted("closest", 21.0285 + 0.005, 105.8542)));

        List<ScoutedPlayerCommand> result = service.findScoutingCandidates(search);

        assertEquals(List.of("closest", "close"), result.stream().map(ScoutedPlayerCommand::username).toList());
        assertEquals(0.6, result.getFirst().distanceKm());
    }

    @Test
    void lookupAddsDistanceOnlyToVisibleProfiles() {
        UUID hiddenId = UUID.randomUUID();
        ScoutedPlayerCommand hidden = ScoutedPlayerCommand.identityOnly(hiddenId, "hidden", "Hidden", null);
        when(profileRepository.findScoutingProfiles(any(), any()))
                .thenReturn(List.of(scouted("visible", 21.0385, 105.8542), hidden));

        List<ScoutedPlayerCommand> result = service.lookupScoutingProfiles(
                SportType.FOOTBALL, 21.0285, 105.8542, List.of(UUID.randomUUID(), hiddenId));

        assertEquals(1.1, result.get(0).distanceKm());
        assertEquals(null, result.get(1).distanceKm());
    }

    private ScoutedPlayerCommand scouted(String username, double lat, double lng) {
        return new ScoutedPlayerCommand(UUID.randomUUID(), username, username, null, true, SkillLevel.INTERMEDIATE,
                1200, Set.of(), null, "Hà Nội", 10.0, 0, 0, 0, 0, 0.0, List.of(), lat, lng, null);
    }

    private SavePlayerSportProfileCommand withDiscoverable(SavePlayerSportProfileCommand c, Boolean discoverable) {
        return new SavePlayerSportProfileCommand(c.sportType(), c.skillLevel(), c.preferredPositions(), c.playStyle(),
                c.city(), c.latitude(), c.longitude(), c.playRadiusKm(), c.availabilities(), discoverable);
    }

    private SavePlayerSportProfileCommand validCommand(List<SaveAvailabilityCommand> availabilities) {
        return new SavePlayerSportProfileCommand(
                SportType.BADMINTON,
                SkillLevel.INTERMEDIATE,
                Set.of("Đánh đôi"),
                "Kiểm soát cầu",
                "Hồ Chí Minh",
                10.762622,
                106.660172,
                15.0,
                availabilities,
                null
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
