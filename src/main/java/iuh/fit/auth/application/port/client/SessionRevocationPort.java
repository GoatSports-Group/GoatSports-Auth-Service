package iuh.fit.auth.application.port.client;

import java.util.UUID;

public interface SessionRevocationPort {
    String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
    String ACCOUNT_DELETION_PENDING = "ACCOUNT_DELETION_PENDING";
    String ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
    /** Tài khoản vừa đăng nhập ở nơi khác: phiên cũ bị thay thế. */
    String SESSION_REPLACED = "SESSION_REPLACED";

    /**
     * Báo các tab đang mở của người dùng (qua websocket của notification-service) rằng phiên đã bị thu hồi.
     * Best effort: lỗi chỉ ghi log, vì gateway vẫn chặn ở request kế tiếp.
     */
    void notifySessionRevoked(UUID userId, String reason);
}
