package iuh.fit.auth.application.port.security;

import iuh.fit.auth.application.exception.ServiceException;

public interface RsaPort {
    String getPublicKey();

    String decrypt(String encryptedText) throws ServiceException;
}
