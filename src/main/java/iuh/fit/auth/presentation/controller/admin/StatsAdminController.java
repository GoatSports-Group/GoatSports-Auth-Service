package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.response.stats.PlatformUserStats;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Số liệu người dùng toàn nền tảng cho dashboard admin. */
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class StatsAdminController {
    private final AdminStatsService adminStatsService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('GET_PLATFORM_STATS')")
    public ResponseEntity<PlatformUserStats> users(@RequestParam LocalDate fromDate,
                                                   @RequestParam LocalDate toDate) throws ServiceException {
        return ResponseEntity.ok(adminStatsService.getUserStats(fromDate, toDate));
    }
}
