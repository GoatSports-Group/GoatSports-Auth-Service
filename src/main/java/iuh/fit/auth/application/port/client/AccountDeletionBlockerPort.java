package iuh.fit.auth.application.port.client;

import iuh.fit.auth.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface AccountDeletionBlockerPort {
    /**
     * Mã các việc còn dang dở chặn việc xóa tài khoản (vé sắp chơi, khoản hoàn tiền đang chạy, đang làm chủ CLB...),
     * gom từ venue-, payment- và club-service. Không hỏi được service nào thì ném lỗi: không biết thì không cho xóa.
     */
    List<String> findBlockers(UUID userId) throws ServiceException;
}
