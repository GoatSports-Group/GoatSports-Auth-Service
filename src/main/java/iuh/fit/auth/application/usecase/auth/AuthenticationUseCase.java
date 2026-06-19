package iuh.fit.auth.application.usecase.auth;

import iuh.fit.auth.application.dto.request.*;
import iuh.fit.auth.application.dto.response.KeycloakInfoResponseCommand;
import iuh.fit.auth.application.dto.response.KeycloakResponseCommand;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.util.BaseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationUseCase {

    private final JwtPort jwtPort;
    private final RedisPort redisPort;
    private final KeycloakPort keycloakPort;
    private final EmailNotificationPort emailNotificationPort;
    private final CurrentUserProvider currentUserProvider;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.verify-code-validity-in-second}")
    private int verificationCodeValidityInSecond;
    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;

    public Map<String, Object> login(LoginRequestCommand loginCommand) throws ServiceException {
        User user = userRepository.findByUsernameOrEmailWithRole(loginCommand.username(), loginCommand.username())
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        String oldRefreshToken = redisPort.getValue("user:" + user.getUserId() + ":refresh");
        if (oldRefreshToken != null) {
            redisPort.deleteKey("user:" + user.getUserId() + ":refresh");
        }

        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        this.redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":refresh",
                refreshToken,
                refreshTokenExpirationSecond,
                TimeUnit.SECONDS
        );

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return result;
    }

    public Map<String, Object> loginWithKeycloak(LinkAccountRequestCommand linkAccountCommand) throws ServiceException {
        KeycloakResponseCommand token = keycloakPort.exchangeCode(linkAccountCommand.code(), linkAccountCommand.redirectUri());

        KeycloakInfoResponseCommand keycloakInfo = keycloakPort.decode(token.accessToken());

        Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));

        User user = userRepository.findByKeycloakId(keycloakInfo.keycloakId())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setKeycloakId(keycloakInfo.keycloakId());
                    newUser.setEmail(keycloakInfo.email());
                    newUser.setUsername(keycloakInfo.username());
                    newUser.setFullName(keycloakInfo.fullName());
                    newUser.setRole(role);
                    newUser.setStatus(UserStatusEnum.ACTIVE);
                    newUser.setAuthProvider(AuthProviderEnum.KEYCLOAK);
                    return userRepository.save(newUser);
                });

        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":refresh",
                refreshToken,
                refreshTokenExpirationSecond,
                TimeUnit.SECONDS
        );

        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":keycloakRefresh",
                token.refreshToken(),
                token.refreshExpiresIn(),
                TimeUnit.SECONDS
        );

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return result;
    }

    public void linkKeycloakAccount(LinkAccountRequestCommand linkAccountCommand) throws ServiceException {
        User user = getCurrentUser();
        if (user.getKeycloakId() != null) {
            throw new ServiceException("Tài khoản đã được liên kết với Keycloak");
        }

        KeycloakResponseCommand token = keycloakPort.exchangeCode(linkAccountCommand.code(), linkAccountCommand.redirectUri());
        KeycloakInfoResponseCommand keycloakInfo = keycloakPort.decode(token.accessToken());

        String keycloakIdStr = keycloakInfo.keycloakId().toString();
        if (keycloakIdStr == null || keycloakIdStr.isEmpty()) {
            keycloakPort.logout(token.refreshToken());
            throw new ServiceException("Không thể lấy thông tin từ Keycloak");
        }

        UUID keycloakId = UUID.fromString(keycloakIdStr);
        boolean checkedKeycloak = userRepository.existsByKeycloakId(keycloakId);
        if (checkedKeycloak) {
            keycloakPort.logout(token.refreshToken());
            throw new ServiceException("Tài khoản Keycloak đã được liên kết với tài khoản khác");
        }

        user.setKeycloakId(keycloakId);
        user.setAuthProvider(AuthProviderEnum.ALL);
        userRepository.save(user);

        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":keycloakRefresh",
                token.refreshToken(),
                token.refreshExpiresIn(),
                TimeUnit.SECONDS);
    }

    public User register(RegisterRequestCommand registerRequestCommand) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(
                registerRequestCommand.username(), registerRequestCommand.username()
        );
        if (existed) throw new ServiceException("Người dùng đã tồn tại");

        String hashedPassword = jwtPort.hashPassword(registerRequestCommand.password());

        User user = new User();
        user.setUsername(registerRequestCommand.username());
        user.setPassword(hashedPassword);
        user.setFullName(registerRequestCommand.fullName());
        user.setEmail(registerRequestCommand.email());
        user.setStatus(UserStatusEnum.INACTIVE);
        user.setAuthProvider(AuthProviderEnum.LOCAL);

        Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
        user.setRole(role);

        User createdUser = userRepository.save(user);

        String verificationCode = BaseUtil.generateVerificationCode();
        this.redisPort.saveWithTTL(
                createdUser.getUserId().toString(),
                verificationCode,
                verificationCodeValidityInSecond,
                TimeUnit.SECONDS);

        emailNotificationPort.sendVerificationEmail(createdUser.getEmail(), verificationCode);

        return createdUser;
    }

    public void verifyUser(VerificationRequestCommand verificationRequestCommand) throws ServiceException {
        User user = userRepository.findByEmail(verificationRequestCommand.email())
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        if (!redisPort.hasKey(user.getUserId().toString())) {
            throw new ServiceException("Mã xác thực đã hết hạn");
        }
        if (!redisPort.getValue(user.getUserId().toString()).equalsIgnoreCase(verificationRequestCommand.verificationCode())) {
            throw new ServiceException("Mã xác thực không chính xác");
        }

        if (user.getAuthProvider() != null && user.getAuthProvider() == AuthProviderEnum.KEYCLOAK) {
            user.setAuthProvider(AuthProviderEnum.ALL);
        }

        user.setStatus(UserStatusEnum.ACTIVE);
        userRepository.save(user);

        redisPort.deleteKey(user.getUserId().toString());
    }

    public void resendVerificationCode(String email) throws ServiceException {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) throw new ServiceException("Người dùng không tồn tại");

        String key = user.getUserId().toString();
        String verificationCode = BaseUtil.generateVerificationCode();
        redisPort.replaceKey(
                key,
                key,
                verificationCode,
                verificationCodeValidityInSecond,
                TimeUnit.SECONDS);
        emailNotificationPort.sendVerificationEmail(email, verificationCode);
    }

    public void logout(String accessToken, String refreshToken) {
        String userId = jwtPort.getUserIdFromToken(refreshToken);
        if (userId == null || userId.isEmpty()) return;

        redisPort.deleteKey("user:" + userId + ":refresh");
        redisPort.saveWithTTL(
                "blacklist:" + accessToken,
                "revoked",
                jwtPort.getRemainingTime(accessToken),
                TimeUnit.SECONDS
        );

        if (redisPort.hasKey("user:" + userId + ":keycloakRefresh")) {
            String keycloakRefresh = redisPort.getValue("user:" + userId + ":keycloakRefresh");
            keycloakPort.logout(keycloakRefresh);
            redisPort.deleteKey("user:" + userId + ":keycloakRefresh");
        }
    }

    public Map<String, Object> refresh(String refreshToken) throws ServiceException {
        String userId = jwtPort.getUserIdFromToken(refreshToken);
        String role = jwtPort.getRoleFromToken(refreshToken);

        if (refreshToken.equalsIgnoreCase("missingValue")) {
            throw new ServiceException("Hết hạn đăng nhập");
        }
        if (!redisPort.hasKey("user:" + userId + ":refresh")
                || !redisPort.getValue("user:" + userId + ":refresh")
                .equalsIgnoreCase(refreshToken))
        {
            throw new ServiceException("Refresh token hết hạn");
        }

        User currentUser = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (currentUser == null)
            throw new ServiceException("Người dùng không tồn tại");
        if (currentUser.getStatus() == UserStatusEnum.INACTIVE)
            throw new ServiceException("Người dùng bị khóa");

        String newAccessToken = jwtPort.generateAccessToken(currentUser);
        String newRefreshToken = jwtPort.generateRefreshToken(currentUser);

        this.redisPort.replaceKey(
                "user:" + userId + ":refresh",
                "user:" + userId + ":refresh",
                newRefreshToken,
                refreshTokenExpirationSecond,
                TimeUnit.SECONDS
        );

        Map<String, Object> result = new HashMap<>();
        result.put("user", currentUser);
        result.put("role", role);
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);

        return result;
    }

    public void forgotPassword(ForgotPasswordRequestCommand forgotPasswordRequestCommand) throws ServiceException {
        User user = userRepository.findByEmail(forgotPasswordRequestCommand.email()).orElse(null);

        if (user == null)
            throw new ServiceException("Người dùng không tồn tại");
        if (user.getStatus() == UserStatusEnum.INACTIVE)
            throw new ServiceException("Người dùng bị khóa");

        String hashedPassword = jwtPort.hashPassword(forgotPasswordRequestCommand.password());
        user.setPassword(hashedPassword);
        user.setStatus(UserStatusEnum.INACTIVE);
        this.userRepository.save(user);

        String verificationCode = BaseUtil.generateVerificationCode();
        redisPort.saveWithTTL(
                user.getUserId().toString(),
                verificationCode,
                verificationCodeValidityInSecond,
                TimeUnit.SECONDS
        );
        emailNotificationPort.sendVerificationEmail(user.getEmail(), verificationCode);
    }

    public User getCurrentUser() throws ServiceException {
        return currentUserProvider.getCurrentUser();
    }
}
