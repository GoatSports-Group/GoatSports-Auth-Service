package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        service = new MatchResultServiceImpl(profileRepository, resultRepository);
    }

    @Test
    void appliesEloAndStatisticsOnce() throws ServiceException {
        var sessionId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var profile = PlayerSportProfile.builder()
                .profileId(UUID.randomUUID()).userId(userId).sportType(SportType.FOOTBALL)
                .eloRating(1200).winCount(0).lossCount(0).drawCount(0).matchCount(0).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.FOOTBALL)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN"
        ));

        assertEquals(1220, result.getNewElo());
        assertEquals(1, profile.getMatchCount());
        assertEquals(1, profile.getWinCount());
        assertEquals(1.0, profile.getWinRate());
    }

    @Test
    void usesLowerEstablishedKFactorForTeamSports() throws ServiceException {
        var sessionId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var profile = PlayerSportProfile.builder()
                .profileId(UUID.randomUUID()).userId(userId).sportType(SportType.FOOTBALL)
                .eloRating(1200).winCount(7).lossCount(5).drawCount(0).matchCount(12).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(profileRepository.findByUserIdAndSportType(userId, SportType.FOOTBALL)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN"
        ));

        assertEquals(1212, result.getNewElo());
    }

    @Test
    void returnsLedgerEntryWithoutApplyingTwice() throws ServiceException {
        var sessionId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var existing = AppliedMatchResult.builder()
                .sessionId(sessionId).userId(userId).oldElo(1200).newElo(1216).build();
        when(resultRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(existing));

        var result = service.apply(new ApplyMatchResultCommand(
                sessionId, userId, SportType.FOOTBALL, 1200, "WIN"
        ));

        assertEquals(1216, result.getNewElo());
        verify(profileRepository, never()).save(any());
    }
}
