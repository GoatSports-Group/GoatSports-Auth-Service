package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.response.stats.PlatformUserStats;
import iuh.fit.auth.application.exception.ServiceException;

import java.time.LocalDate;

public interface AdminStatsService {
    PlatformUserStats getUserStats(LocalDate fromDate, LocalDate toDate) throws ServiceException;
}
