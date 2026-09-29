package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.response.stats.PlatformUserStats;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Test
    void countsEveryRoleAndStatusAndSpreadsNewUsersOverEachDay() throws ServiceException {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 3);
        when(userRepository.countByRole()).thenReturn(Map.of("PLAYER", 40L, "VENUE_OWNER", 5L));
        when(userRepository.countByStatus()).thenReturn(Map.of("ACTIVE", 42L, "BLOCKED", 3L));
        when(userRepository.findCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(from.atTime(8, 0), from.atTime(21, 0), to.atTime(9, 0)));

        PlatformUserStats stats = new AdminStatsServiceImpl(userRepository).getUserStats(from, to);

        assertEquals(45, stats.total());
        assertEquals(0L, stats.byRole().get("ADMIN"));
        assertEquals(0L, stats.byStatus().get("PENDING"));
        assertEquals(3, stats.newUsers());
        assertEquals(List.of(2L, 0L, 1L), stats.newByDay().stream().map(PlatformUserStats.DayCount::count).toList());
    }

    @Test
    void rejectsReversedRanges() {
        assertThrows(ServiceException.class, () -> new AdminStatsServiceImpl(userRepository)
                .getUserStats(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1)));
    }
}
