package iuh.fit.auth.component;

import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import iuh.fit.auth.entity.Permission;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.enums.RoleEnum;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.repository.UserRepository;
import iuh.fit.auth.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler
    ) throws Exception
    {
        String userId = JwtService.getCurrentUserId().isPresent() ? JwtService.getCurrentUserId().get() : "";
        if (userId.isEmpty()) {
            throw new ServiceException("Bạn chưa đăng nhập");
        }

        String bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String requestUri = request.getRequestURI();
        String path = (bestPattern != null && !"/**".equals(bestPattern)) ? bestPattern : requestUri;

        String method = request.getMethod();

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại trong hệ thống"));

        Role role = user.getRole();

        if (role == null)
            throw new ServiceException("Bạn không có vai trò");

        log.info("Check role of user: {}", role.getName());

        if (RoleEnum.ADMIN.name().equalsIgnoreCase(role.getName())) return true;

        Set<Permission> permissions = role.getPermissions();
        boolean hasPermission = permissions.stream().anyMatch(
                p -> p.getApiPath().equals(path) && p.getMethod().equals(method));
        if (!hasPermission) {
            throw new ServiceException("Bạn không có quyền hạn để truy cập");
        }

        return true;
    }
}
