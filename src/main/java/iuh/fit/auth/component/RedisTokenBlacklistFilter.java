package iuh.fit.auth.component;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RedisTokenBlacklistFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenBlacklistFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private final String[] whiteList = {
//          Endpoint về authentication
            "/api/v1/auth/login", "/api/v1/auth/login/keycloak", "/api/v1/auth/register/**",
            "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/verify", "/api/v1/auth/resend",

//          Endpoint về kiểm tra
            "/actuator/**"
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (Arrays.stream(whiteList).anyMatch(requestURI::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getCookies() != null) {
            String token = Arrays.stream(request.getCookies())
                    .filter(c -> c.getName().equalsIgnoreCase("accessToken"))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);

            if (token != null && Boolean.TRUE.equals(this.redisTemplate.hasKey("blacklist:" + token))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token đã bị thu hồi");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
