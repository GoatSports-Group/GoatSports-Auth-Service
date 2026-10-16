package iuh.fit.auth.application.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mật khẩu đúng nhưng tài khoản chưa dùng được. Chỉ ném SAU khi đã xác thực mật khẩu, nên trạng thái tài khoản không lộ
 * cho người không biết mật khẩu. {@code code} để client chọn popup: ACCOUNT_INACTIVE (kích hoạt bằng OTP) hoặc
 * ACCOUNT_BLOCKED (gửi khiếu nại cho quản trị viên bằng {@code appealToken}).
 */
public class AccountStatusException extends ServiceException {
    public static final String INACTIVE = "ACCOUNT_INACTIVE";
    public static final String BLOCKED = "ACCOUNT_BLOCKED";

    private final String code;
    private final Map<String, Object> data;

    private AccountStatusException(String code, String message, Map<String, Object> data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public static AccountStatusException inactive(String email, boolean codeSent) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", email);
        data.put("codeSent", codeSent);
        return new AccountStatusException(INACTIVE, "Tài khoản chưa được kích hoạt", data);
    }

    public static AccountStatusException blocked(String email, String fullName, String appealToken, long appealTtlMinutes) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", email);
        data.put("fullName", fullName);
        data.put("appealToken", appealToken);
        data.put("appealTtlMinutes", appealTtlMinutes);
        return new AccountStatusException(BLOCKED, "Tài khoản đã bị khóa", data);
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
