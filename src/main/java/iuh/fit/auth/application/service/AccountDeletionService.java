package iuh.fit.auth.application.service;

import iuh.fit.auth.application.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Người dùng tự xóa tài khoản: gửi mã qua email, xác nhận thì tài khoản chuyển sang chờ xóa và bị đăng xuất; đăng nhập
 * lại trong {@link #GRACE_DAYS} ngày là hủy (xem AuthenticationServiceImpl). Hết hạn thì job gọi {@link #purge}.
 */
public interface AccountDeletionService {
    int GRACE_DAYS = 30;

    /** Các việc còn dang dở chặn xóa (mã, client dịch ra lời), rỗng là xóa được. */
    List<String> currentBlockers() throws ServiceException;

    void sendCode() throws ServiceException;

    /** Trả về ngày tài khoản bị xóa thật. */
    LocalDateTime requestDeletion(String code) throws ServiceException;

    List<UUID> findDueForPurge(int limit);

    /** Xóa (soft delete) một tài khoản đã quá hạn chờ, xóa trên Keycloak và báo các service khác qua Kafka. */
    void purge(UUID userId) throws ServiceException;
}
