package iuh.fit.auth.util;

import java.security.SecureRandom;

public class BaseUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateVerificationCode() {
        int code = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }
}
