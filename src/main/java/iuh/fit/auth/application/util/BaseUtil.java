package iuh.fit.auth.application.util;

import iuh.fit.auth.application.exception.ServiceException;

import java.security.SecureRandom;
import java.util.UUID;

public class BaseUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateVerificationCode() {
        int code = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }

    public static UUID uuid(String id) throws ServiceException {
        UUID uuid;

        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id không hợp lệ: không phải UUID");
        }

        return uuid;
    }
}
