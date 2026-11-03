package iuh.fit.auth.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaPasswordCipherTest {

    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptedPasswordDecryptsWithTheFormatTheKeycloakExtensionUses() throws Exception {
        KafkaPasswordCipher cipher = new KafkaPasswordCipher(KEY);

        String encrypted = cipher.encrypt("Mật-khẩu@123");

        assertThat(encrypted).doesNotContain("Mật-khẩu@123");
        assertThat(cipher.encrypt("Mật-khẩu@123")).isNotEqualTo(encrypted);
        // Giống hệt KafkaEventListenerProviderFactory.decryptPassword trong keycloak-kafka-listener.
        byte[] payload = Base64.getDecoder().decode(encrypted);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(KEY), "AES"),
                new GCMParameterSpec(128, payload, 0, 12));
        assertThat(new String(aes.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8))
                .isEqualTo("Mật-khẩu@123");
    }

    @Test
    void rejectsKeyThatIsNotAes256() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new KafkaPasswordCipher(shortKey)).isInstanceOf(IllegalStateException.class);
    }
}
