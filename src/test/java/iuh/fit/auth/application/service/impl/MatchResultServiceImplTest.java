package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceImplTest {
    @Mock PlayerSportProfileRepository profileRepository;
    @Mock AppliedMatchResultRepository resultRepository;
    private MatchResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MatchResultServiceImpl(profileRepository, resultRepository, null);
    }

    @Test
    void appliesEloAndStatisticsOnce() throws ServiceException {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlayerSportProfile profile = PlayerSportProfile.builder()
                .profileId(UUID.randomUUID()).userId(userId).sportType(SportType.FOOTBALL)
                .eloRating(1200).winCount(0).lossCount(0).drawCount(0).matchCount(0).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.FOOTBALL)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppliedMatchResult result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN", null
        ));

        assertEquals(1220, result.getNewElo());
        assertEquals(1, profile.getMatchCount());
        assertEquals(1, profile.getWinCount());
        assertEquals(1.0, profile.getWinRate());
    }

    @Test
    void usesLowerEstablishedKFactorForTeamSports() throws ServiceException {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlayerSportProfile profile = PlayerSportProfile.builder()
                .profileId(UUID.randomUUID()).userId(userId).sportType(SportType.FOOTBALL)
                .eloRating(1200).winCount(7).lossCount(5).drawCount(0).matchCount(12).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.FOOTBALL)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppliedMatchResult result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN", null
        ));

        assertEquals(1212, result.getNewElo());
    }

    @Test
    void createsTheMissingSportProfileSeededWithTheEloTheMatchWasPlayedAt() throws ServiceException {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppliedMatchResult result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.BADMINTON, 1200, "WIN", 1500
        ));

        ArgumentCaptor<PlayerSportProfile> captor = ArgumentCaptor.forClass(PlayerSportProfile.class);
        verify(profileRepository).save(captor.capture());
        PlayerSportProfile created = captor.getValue();
        assertEquals(SportType.BADMINTON, created.getSportType());
        assertEquals(1500, result.getOldElo());
        assertEquals(1, created.getMatchCount());
        // Elo tăng sau trận thắng nên trình độ phải tự lên ADVANCED, không giữ INTERMEDIATE mặc định.
        assertEquals(SkillLevel.ADVANCED, created.getSkillLevel());
    }

    @Test
    void movesTheSkillLevelUpWhenEloCrossesTheThreshold() throws ServiceException {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlayerSportProfile profile = PlayerSportProfile.builder()
                .profileId(UUID.randomUUID()).userId(userId).sportType(SportType.BADMINTON)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .eloRating(1340).winCount(3).lossCount(1).drawCount(0).matchCount(4).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.BADMINTON)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.BADMINTON, 1340, "WIN", 1340
        ));

        assertEquals(1360, profile.getEloRating());
        assertEquals(SkillLevel.ADVANCED, profile.getSkillLevel());
    }

    @Test
    void returnsLedgerEntryWithoutApplyingTwice() throws ServiceException {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppliedMatchResult existing = AppliedMatchResult.builder()
                .sessionId(sessionId).userId(userId).oldElo(1200).newElo(1216).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(existing));

        AppliedMatchResult result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN", null
        ));

        assertEquals(1216, result.getNewElo());
        verify(profileRepository, never()).save(any());
    }
}
