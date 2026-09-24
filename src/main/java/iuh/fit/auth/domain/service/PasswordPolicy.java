package iuh.fit.auth.domain.service;

import java.util.regex.Pattern;

/** Độ mạnh mật khẩu — một quy tắc duy nhất cho đăng ký, quên mật khẩu, đổi/tạo mật khẩu và admin đặt lại. */
public final class PasswordPolicy {

    public static final String STRENGTH_MESSAGE =
            "Mật khẩu phải có ít nhất 8 ký tự, chữ hoa, chữ thường, số và ký tự đặc biệt";

    /** Ít nhất 8 ký tự, có chữ thường, chữ hoa, chữ số và ít nhất một ký tự không phải chữ/số. */
    private static final Pattern STRONG = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private PasswordPolicy() {
    }

    public static boolean isStrong(String rawPassword) {
        return rawPassword != null && STRONG.matcher(rawPassword).matches();
    }
}
