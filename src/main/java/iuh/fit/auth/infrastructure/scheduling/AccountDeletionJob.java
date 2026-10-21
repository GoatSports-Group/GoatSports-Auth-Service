package iuh.fit.auth.infrastructure.scheduling;

import iuh.fit.auth.application.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Mỗi giờ xóa thật các tài khoản đã chờ đủ 30 ngày. Mỗi tài khoản một giao dịch: một cái lỗi không kéo cái khác. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionJob {
    private static final int BATCH = 50;

    private final AccountDeletionService accountDeletionService;

    @Scheduled(fixedDelayString = "${app.account-deletion.purge-delay-ms}", initialDelay = 300_000)
    public void purgeDueAccounts() {
        for (UUID userId : accountDeletionService.findDueForPurge(BATCH)) {
            try {
                accountDeletionService.purge(userId);
            } catch (Exception e) {
                log.warn("Could not purge account {}, retrying next run: {}", userId, e.getMessage());
            }
        }
    }
}
