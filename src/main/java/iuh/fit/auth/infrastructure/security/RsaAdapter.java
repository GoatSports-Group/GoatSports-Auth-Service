package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.security.RsaPort;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
public class RsaAdapter implements RsaPort {
    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.keyPair = keyGen.generateKeyPair();
            log.info("RSA KeyPair (2048-bit) initialized successfully in auth-service");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initialize RSA key pair: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPublicKey() {
        if (keyPair == null) {
            init();
        }
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @Override
    public String decrypt(String encryptedText) throws ServiceException {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }

        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText.trim());
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("RSA decryption failed or string was plain text: {}", e.getMessage());
            return encryptedText;
        }
    }
}
