package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.util.UUID;

/**
 * Tài khoản bị khóa gửi thư cho quản trị viên ngay từ màn đăng nhập; quản trị viên đọc, trả lời qua email và đánh dấu
 * đã xử lý. Người gửi chứng minh là chủ tài khoản bằng mã khiếu nại chỉ cấp sau khi nhập đúng mật khẩu.
 */
public interface AccountAppealService {
    int SUBJECT_MAX = 150;
    int MESSAGE_MIN = 10;
    int MESSAGE_MAX = 4000;

    /** Cấp mã khiếu nại (dùng một lần, có hạn) cho tài khoản bị khóa vừa nhập đúng mật khẩu. */
    String issueAppealToken(UUID userId);

    long appealTokenTtlMinutes();

    AccountAppeal submit(String appealToken, String subject, String message) throws ServiceException;

    PageResult<AccountAppeal> list(AccountAppealStatus status, int page, int pageSize);

    long unreadCount();

    /** Mở thư: thư mới chuyển sang "đã đọc". Kèm trạng thái hiện tại của tài khoản để biết còn cần mở khóa không. */
    AppealDetail open(UUID appealId) throws ServiceException;

    AccountAppeal reply(UUID appealId, String reply, boolean resolve) throws ServiceException;

    AccountAppeal changeStatus(UUID appealId, AccountAppealStatus status) throws ServiceException;

    record AppealDetail(AccountAppeal appeal, UserStatusEnum accountStatus) {
    }
}
