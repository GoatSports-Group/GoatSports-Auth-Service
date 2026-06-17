package iuh.fit.auth.service;

import iuh.fit.auth.dto.response.authentication.UserResponse;
import com.nimbusds.jose.util.Base64;
import iuh.fit.auth.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Getter
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;
    @Getter
    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;

    public String generateAccessToken(UserResponse user) {
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
            log.error("Không thể tạo token: " + e.getMessage());
            throw e;
        }
    }

    public String generateRefreshToken(UserResponse user) {
        try {
            Instant now = Instant.now();
            Instant validity = now.plus(refreshTokenExpirationSecond, ChronoUnit.SECONDS);

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
            log.error("Không thể tạo token: " + e.getMessage());
            throw e;
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtSecret).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    public Jwt checkValidToken(String refreshToken) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        try {
            return decoder.decode(refreshToken);
        } catch (Exception e) {
            log.error(">>> Lỗi JWT: " + e.getMessage());
            throw e;
        }
    }

    public long getRemainingTime(String token) {
        Jwt jwt = checkValidToken(token);

        Instant expiration = jwt.getExpiresAt();
        Instant now = Instant.now();
        if (expiration == null)
            return 0;
        long seconds = Duration.between(now, expiration).toSeconds();

        return Math.max(seconds, 0);
    }

    public static Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof String userId) {
            return Optional.of(userId);
        }
        return Optional.empty();
    }


    public static Optional<String> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Current authentication: {}", Optional.ofNullable(extractPrincipal(authentication)));
        return Optional.ofNullable(extractPrincipal(authentication));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof User user) {
            return user.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }
}
