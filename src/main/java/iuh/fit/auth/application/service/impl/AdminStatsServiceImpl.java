package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.response.stats.PlatformUserStats;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.service.AdminStatsService;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AdminStatsServiceImpl implements AdminStatsService {
    private static final int MAX_RANGE_DAYS = 366;

    private final UserRepository userRepository;

    public AdminStatsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PlatformUserStats getUserStats(LocalDate fromDate, LocalDate toDate) throws ServiceException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ServiceException("Khoảng ngày thống kê không hợp lệ.");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            throw new ServiceException("Khoảng ngày thống kê tối đa 366 ngày.");
        }
        Map<String, Long> byRole = zeroed(RoleEnum.values());
        byRole.putAll(userRepository.countByRole());
        Map<String, Long> byStatus = zeroed(UserStatusEnum.values());
        byStatus.putAll(userRepository.countByStatus());

        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (LocalDate day = fromDate; !day.isAfter(toDate); day = day.plusDays(1)) perDay.put(day, 0L);
        List<LocalDateTime> created = userRepository.findCreatedAtBetween(fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());
        created.forEach(time -> perDay.merge(time.toLocalDate(), 1L, Long::sum));
        List<PlatformUserStats.DayCount> byDay = new ArrayList<>();
        perDay.forEach((day, count) -> byDay.add(new PlatformUserStats.DayCount(day, count)));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new PlatformUserStats(fromDate, toDate, total, byRole, byStatus, created.size(), byDay);
    }

    private static Map<String, Long> zeroed(Enum<?>[] values) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Enum<?> value : values) map.put(value.name(), 0L);
        return map;
    }
}
