package iuh.fit.auth.application.dto.response.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Người dùng toàn nền tảng cho dashboard admin: tổng theo vai trò / trạng thái và người mới theo ngày trong kỳ. */
public record PlatformUserStats(
        LocalDate fromDate,
        LocalDate toDate,
        long total,
        Map<String, Long> byRole,
        Map<String, Long> byStatus,
        long newUsers,
        List<DayCount> newByDay
) {
    public record DayCount(LocalDate date, long count) { }
}
