package iuh.fit.auth.config;

import iuh.fit.auth.component.AccessDeniedHandlerCustom;
import iuh.fit.auth.component.RedisTokenBlacklistFilter;
import iuh.fit.auth.component.SecurityHeaderFilter;
import iuh.fit.auth.enums.RoleEnum;
import iuh.fit.auth.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import lombok.RequiredArgsConstructor;
import com.nimbusds.jose.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final String[] whiteList = {
//          Endpoint về authentication
            "/api/v1/auth/login", "/api/v1/auth/login/keycloak", "/api/v1/auth/register/**",
            "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/verify", "/api/v1/auth/resend",

//          Endpoint về kiểm tra
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AccessDeniedHandlerCustom accessDeniedHandlerCustom,
            RedisTokenBlacklistFilter redisBlacklistFilter, SecurityHeaderFilter securityHeaderFilter
    ) throws Exception
    {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.accessDeniedHandler(accessDeniedHandlerCustom))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(login -> login.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/role").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers("/api/v1/permissions/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/roles/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/roles/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(whiteList).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(securityHeaderFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(redisBlacklistFilter, SecurityHeaderFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(JwtService.JWT_ALGORITHM)
                .build();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtSecret).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JwtService.JWT_ALGORITHM.getName());
    }
}
