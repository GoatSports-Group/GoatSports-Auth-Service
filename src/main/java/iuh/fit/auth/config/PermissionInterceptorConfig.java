package iuh.fit.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import iuh.fit.auth.component.PermissionInterceptor;

@Configuration
@RequiredArgsConstructor
public class PermissionInterceptorConfig implements WebMvcConfigurer {
    private final PermissionInterceptor permissionInterceptor;

    private final String[] whiteList = {
            // Các endpoint về authentication
            "/api/v1/auth/**", "/api/v1/users/**",

            // Các endpoint về role để người dùng có thể xem danh sách và chi tiết vai trò
            "/api/v1/roles/{id}",
            "/api/v1/roles",
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .excludePathPatterns(whiteList);
    }
}
