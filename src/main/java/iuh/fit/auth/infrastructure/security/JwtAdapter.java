package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.domain.entity.User;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtAdapter implements JwtPort {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;
    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;
    @Value("${app.verify-code-validity-in-second}")
    private int verificationCodeValidityInSecond;

    @Override
    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpirationSecond);
    }

    @Override
    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpirationSecond);
    }

    @Override
    public String getUserIdFromToken(String token) {
        Jwt jwt = checkValidToken(token);
        return jwt.getSubject();
    }

    @Override
    public String getRoleFromToken(String token) {
        Jwt jwt = checkValidToken(token);
        return jwt.getClaimAsString("role");
    }

    @Override
    public long getRemainingTime(String token) {
        Jwt jwt = checkValidToken(token);

        Instant expiration = jwt.getExpiresAt();
        Instant now = Instant.now();
        if (expiration == null)
            return 0;
        long seconds = Duration.between(now, expiration).toSeconds();

        return Math.max(seconds, 0);
    }

    @Override
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public AuthProperties getAuthProperties() {
        return new AuthProperties(
                verificationCodeValidityInSecond,
                refreshTokenExpirationSecond
        );
    }

    private String buildToken(User user, long accessTokenExpirationSecond) {
        try {
            Instant now = Instant.now();
            Instant validity = now.plus(accessTokenExpirationSecond, ChronoUnit.SECONDS);

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuedAt(now)
                    .expiresAt(validity)
                    .subject(user.getUserId().toString())
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().getName())
                    .build();

            JwsHeader header = JwsHeader.with(JWT_ALGORITHM).build();
            return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        } catch (Exception e) {
            log.error("Không thể tạo token: {}", e.getMessage());
            throw e;
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtSecret).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    private Jwt checkValidToken(String refreshToken) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        try {
            return decoder.decode(refreshToken);
        } catch (Exception e) {
            log.error(">>> Lỗi JWT: {}", e.getMessage());
            throw e;
        }
    }
}
