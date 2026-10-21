package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.domain.service.PasswordPolicy;
import iuh.fit.auth.application.dto.request.auth.*;
import iuh.fit.auth.application.dto.response.auth.KeycloakParseCommand;
import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.client.SessionRevocationPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.application.service.AccountAppealService;
import iuh.fit.auth.application.exception.AccountStatusException;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.application.port.security.RsaPort;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.util.BaseUtil;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.auth.application.dto.response.auth.RefreshResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AuthenticationServiceImpl implements AuthenticationService {
    /**
     * Khóa Redis của phiên đăng nhập. Hai tiền tố blacklist là HỢP ĐỒNG với api-gateway
     * ({@code RedisTokenBlacklistFilter} chặn token có khóa này) — đổi tên ở đây phải đổi cả bên gateway.
     */
    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_BLACKLIST_PREFIX = "blacklist_refresh:";
    /** OTP kích hoạt gửi khi tài khoản chưa kích hoạt đăng nhập đúng mật khẩu: tối đa ngần này lần mỗi giờ. */
    private static final String LOGIN_OTP_RATE_PREFIX = "rate:login-activation-otp:";
    private static final int LOGIN_OTP_MAX_PER_HOUR = 3;

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final JwtPort jwtPort;
    private final RedisPort redisPort;
    private final KeycloakPort keycloakPort;
    private final EmailNotificationPort emailNotificationPort;
    private final CurrentUserProvider currentUserProvider;
    private final UserRegistrationEventPublisher userRegistrationEventPublisher;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RsaPort rsaPort;
    private final AccountAppealService accountAppealService;
    private final SessionRevocationPort sessionRevocationPort;

    public AuthenticationServiceImpl(
            JwtPort jwtPort, RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort, CurrentUserProvider currentUserProvider,
            UserRegistrationEventPublisher userRegistrationEventPublisher,
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper,
            RsaPort rsaPort, AccountAppealService accountAppealService, SessionRevocationPort sessionRevocationPort) {
        this.jwtPort = jwtPort;
        this.redisPort = redisPort;
        this.keycloakPort = keycloakPort;
        this.emailNotificationPort = emailNotificationPort;
        this.currentUserProvider = currentUserProvider;
        this.userRegistrationEventPublisher = userRegistrationEventPublisher;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.rsaPort = rsaPort;
        this.accountAppealService = accountAppealService;
        this.sessionRevocationPort = sessionRevocationPort;
    }

    @Override
    public UserResponseCommand register(RegisterRequestCommand registerRequestCommand) throws ServiceException {
        RoleEnum registrationRole = registerRequestCommand.accountType();
        if (registrationRole != RoleEnum.PLAYER && registrationRole != RoleEnum.VENUE_OWNER) {
            throw new ServiceException("Loại tài khoản đăng ký không hợp lệ");
        }
        return registerWithRole(registerRequestCommand, registrationRole);
    }

    /**
     * QUAN TRỌNG: hàm này KHÔNG kiểm tra mật khẩu — {@code AuthenticationController} đã xác thực bằng
     * AuthenticationManager của Spring Security trước khi gọi. Không được gọi hàm này từ nơi khác khi chưa xác thực.
     */
    @Override
    public LoginResponseCommand login(LoginRequestCommand loginCommand) throws ServiceException {
        User user = userRepository.findByUsernameOrEmailWithRole(loginCommand.username(), loginCommand.username())
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        if (user.getStatus() == UserStatusEnum.INACTIVE) {
            // Mật khẩu đúng: chắc chắn là chủ tài khoản, gửi luôn OTP kích hoạt (có giới hạn để không spam hộp thư).
            throw inactive(user);
        }
        if (user.getStatus() == UserStatusEnum.BLOCKED) {
            throw AccountStatusException.blocked(user.getEmail(), user.getFullName(),
                    accountAppealService.issueAppealToken(user.getUserId()), accountAppealService.appealTokenTtlMinutes());
        }

        return issueSession(user, restoreIfPendingDeletion(user));
    }

    @Override
    public void verifyUser(VerificationRequestCommand verificationRequestCommand) throws ServiceException {
        User user = userRepository.findByEmail(verificationRequestCommand.email())
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        if (!redisPort.hasKey(user.getUserId().toString())) {
            throw new ServiceException("Mã xác thực đã hết hạn");
        }
        if (!redisPort.getValue(user.getUserId().toString())
                .equalsIgnoreCase(verificationRequestCommand.verificationCode())) {
            throw new ServiceException("Mã xác thực không chính xác");
        }

        user.setStatus(UserStatusEnum.ACTIVE);
        User savedUser = userRepository.save(user);

        userRegistrationEventPublisher.publishUserVerified(savedUser);

        redisPort.deleteKey(savedUser.getUserId().toString());
        // Admin từng bỏ xác thực tài khoản này thì gateway đang chặn: nhập OTP xong là dùng lại được.
        redisPort.deleteKey(blockedUserKey(savedUser.getUserId()));
    }

    @Override
    public void resendVerificationCode(String email) throws ServiceException {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            throw new ServiceException("Người dùng không tồn tại");

        String key = user.getUserId().toString();
        String verificationCode = BaseUtil.generateVerificationCode();

        redisPort.replaceKey(
                key,
                key,
                verificationCode,
                jwtPort.getAuthProperties().verificationCodeValidityInSecond(),
                TimeUnit.SECONDS);

        emailNotificationPort.sendVerificationEmail(email, verificationCode);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        String userId = jwtPort.getUserIdFromToken(refreshToken);
        if (userId == null || userId.isEmpty())
            return;

        redisPort.deleteKey(refreshKey(userId));
        redisPort.saveWithTTL(
                ACCESS_BLACKLIST_PREFIX + accessToken,
                "revoked",
                jwtPort.getRemainingTime(accessToken),
                TimeUnit.SECONDS);

        if (redisPort.hasKey(keycloakRefreshKey(userId))) {
            String keycloakRefresh = redisPort.getValue(keycloakRefreshKey(userId));
            try {
                keycloakPort.logout(keycloakRefresh);
            } catch (Exception e) {
                log.warn("Keycloak logout failed or unreachable: {}. Proceeding with local logout.", e.getMessage());
            }
            redisPort.deleteKey(keycloakRefreshKey(userId));
        }
    }

    @Override
    public RefreshResponseCommand refresh(String refreshToken) throws ServiceException {
        if (refreshToken == null || refreshToken.isBlank() || refreshToken.equalsIgnoreCase("missingValue")) {
            throw new ServiceException("Hết hạn đăng nhập");
        }

        String userId;
        String role;
        try {
            userId = jwtPort.getUserIdFromToken(refreshToken);
            role = jwtPort.getRoleFromToken(refreshToken);
        } catch (Exception e) {
            throw new ServiceException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String refreshKey = refreshKey(userId);
        String keycloakRefreshKey = keycloakRefreshKey(userId);

        String blacklistKey = REFRESH_BLACKLIST_PREFIX + refreshToken;
        if (redisPort.hasKey(blacklistKey)) {
            redisPort.deleteKey(refreshKey);
            redisPort.deleteKey(keycloakRefreshKey);

            log.warn("Security Alert: Replay attack detected for user ID {}. Invalidated all active refresh tokens.",
                    userId);

            throw new ServiceException("Cảnh báo bảo mật: Phiên làm việc đã bị xâm phạm. Vui lòng đăng nhập lại.");
        }

        if (!redisPort.hasKey(refreshKey) || !redisPort.getValue(refreshKey).equalsIgnoreCase(refreshToken)) {
            throw new ServiceException("Refresh token đã hết hạn");
        }

        if (redisPort.hasKey(keycloakRefreshKey)) {
            String keycloakRefresh = redisPort.getValue(keycloakRefreshKey);
            try {
                KeycloakResponseCommand keycloakResponseCommand = keycloakPort.refreshKeycloakToken(keycloakRefresh);
                if (keycloakResponseCommand != null && keycloakResponseCommand.refreshToken() != null) {
                    redisPort.replaceKey(
                            keycloakRefreshKey,
                            keycloakRefreshKey,
                            keycloakResponseCommand.refreshToken(),
                            keycloakResponseCommand.refreshExpiresIn(),
                            TimeUnit.SECONDS);
                } else {
                    log.warn("Keycloak session expired during refresh. Proceeding with local token refresh only.");
                }
            } catch (Exception e) {
                log.warn("Keycloak is unreachable during token refresh: {}. Proceeding with local token refresh only.",
                        e.getMessage());
            }
        }

        User currentUser = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (currentUser == null)
            throw new ServiceException("Người dùng không tồn tại");
        if (currentUser.getStatus() == UserStatusEnum.INACTIVE || currentUser.getStatus() == UserStatusEnum.BLOCKED
                || currentUser.getStatus() == UserStatusEnum.PENDING_DELETION
                || currentUser.getStatus() == UserStatusEnum.DELETED)
            throw new ServiceException("Người dùng bị khóa");

        String newAccessToken = jwtPort.generateAccessToken(currentUser);
        String newRefreshToken = jwtPort.generateRefreshToken(currentUser);

        this.redisPort.replaceKey(
                refreshKey,
                refreshKey,
                newRefreshToken,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(),
                TimeUnit.SECONDS);

        long remainingTime = jwtPort.getRemainingTime(refreshToken);
        if (remainingTime > 0) {
            redisPort.saveWithTTL(
                    blacklistKey,
                    "used",
                    remainingTime,
                    TimeUnit.SECONDS);
        }

        return new RefreshResponseCommand(userMapper.toUserResponseCommand(currentUser), role, newAccessToken,
                newRefreshToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestCommand forgotPasswordRequestCommand) throws ServiceException {
        User user = userRepository.findByEmail(forgotPasswordRequestCommand.email()).orElse(null);

        if (user == null)
            throw new ServiceException("Người dùng không tồn tại");
        if (user.getStatus() == UserStatusEnum.INACTIVE)
            throw new ServiceException("Người dùng bị khóa");

        String rawPassword = decryptNewPassword(forgotPasswordRequestCommand.password(), forgotPasswordRequestCommand.confirmPassword());

        String hashedPassword = jwtPort.hashPassword(rawPassword);
        user.setPassword(hashedPassword);
        user.setStatus(UserStatusEnum.INACTIVE);
        User savedUser = this.userRepository.save(user);

        userRegistrationEventPublisher.publishUserPasswordUpdated(savedUser, rawPassword);

        String verificationCode = BaseUtil.generateVerificationCode();
        redisPort.saveWithTTL(
                savedUser.getUserId().toString(),
                verificationCode,
                jwtPort.getAuthProperties().verificationCodeValidityInSecond(),
                TimeUnit.SECONDS);

        emailNotificationPort.sendVerificationEmail(savedUser.getEmail(), verificationCode);
    }

    @Override
    public UserResponseCommand getCurrentUser() throws ServiceException {
        User currentUser = currentUserProvider.getCurrentUser();
        return userMapper.toUserResponseCommand(currentUser);
    }

    @Override
    public LoginResponseCommand callback(String code, String redirectUri) throws ServiceException {
        KeycloakResponseCommand keycloakResponse = keycloakPort.exchangeCode(code, redirectUri);
        if (keycloakResponse == null || keycloakResponse.accessToken() == null) {
            throw new ServiceException("Failed to exchange code with Keycloak");
        }

        KeycloakParseCommand keycloakParsed = jwtPort.decodeKeycloakToken(keycloakResponse.accessToken());
        String email = keycloakParsed.email();
        String username = keycloakParsed.username();
        String fullName = keycloakParsed.fullName();
        UUID keycloakId = keycloakParsed.keycloakId();

        if (keycloakId == null) {
            throw new ServiceException("Keycloak access token does not contain a sub claim");
        }

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> userRepository.findByUsernameOrEmailWithRole(
                        username, email).orElse(null));

        if (user == null) {
            user = new User();
            user.setUsername(username != null ? username : email);
            user.setFullName(fullName != null ? fullName : (username != null ? username : email));
            user.setEmail(email);
            user.setKeycloakId(keycloakId);
            user.setStatus(UserStatusEnum.ACTIVE);

            Role defualtRole = roleRepository.findByName(RoleEnum.PLAYER.name())
                    .orElseThrow(() -> new ServiceException("Default role PLAYER not found"));
            user.setRole(defualtRole);

            user.getAuthProviders().add(AuthProviderEnum.GOOGLE);
            user.getAuthProviders().add(AuthProviderEnum.LOCAL);

            user = userRepository.save(user);
        } else {
            if (user.getKeycloakId() == null) {
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
            }
        }

        // Google không được là cửa sau của lệnh khóa: không cấp phiên, chỉ cấp mã gửi khiếu nại như đăng nhập mật khẩu.
        if (user.getStatus() == UserStatusEnum.BLOCKED) {
            throw AccountStatusException.blocked(user.getEmail(), user.getFullName(),
                    accountAppealService.issueAppealToken(user.getUserId()), accountAppealService.appealTokenTtlMinutes());
        }
        // Chưa xác thực (vừa đăng ký, hoặc admin bỏ xác thực): Google đã chứng minh chủ email, gửi OTP như đăng nhập
        // mật khẩu. Keycloak không còn tắt user nữa nên luồng Google luôn về tới đây thay vì kẹt ở trang lỗi Keycloak.
        if (user.getStatus() == UserStatusEnum.INACTIVE) {
            throw inactive(user);
        }

        String oldKeycloakRefresh = redisPort.getValue(keycloakRefreshKey(user.getUserId()));
        if (oldKeycloakRefresh != null) {
            redisPort.deleteKey(keycloakRefreshKey(user.getUserId()));
        }
        redisPort.saveWithTTL(
                keycloakRefreshKey(user.getUserId()),
                keycloakResponse.refreshToken(),
                keycloakResponse.refreshExpiresIn(),
                TimeUnit.SECONDS);

        return issueSession(user, restoreIfPendingDeletion(user));
    }

    /** Tài khoản chưa xác thực: gửi OTP kích hoạt (tối đa {@value #LOGIN_OTP_MAX_PER_HOUR} lần/giờ) và báo client mở popup. */
    private AccountStatusException inactive(User user) throws ServiceException {
        boolean sent = redisPort.incrementWithTtl(LOGIN_OTP_RATE_PREFIX + user.getUserId(), 1, TimeUnit.HOURS)
                <= LOGIN_OTP_MAX_PER_HOUR;
        if (sent) {
            resendVerificationCode(user.getEmail());
        }
        return AccountStatusException.inactive(user.getEmail(), sent);
    }

    /**
     * Đăng nhập lại (mật khẩu hoặc Google) trong thời gian chờ xóa là hủy yêu cầu xóa, như Facebook. Mật khẩu / Google
     * đã được xác thực trước khi tới đây nên chắc chắn là chủ tài khoản.
     */
    private boolean restoreIfPendingDeletion(User user) throws ServiceException {
        if (user.getStatus() != UserStatusEnum.PENDING_DELETION) {
            return false;
        }
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setDeletionRequestedAt(null);
        userRepository.save(user);
        redisPort.deleteKey(blockedUserKey(user.getUserId()));
        try {
            emailNotificationPort.sendAccountDeletionCancelled(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.warn("Could not send deletion-cancelled email to user {}: {}", user.getUserId(), e.getMessage());
        }
        return true;
    }

    private UserResponseCommand registerWithRole(
            RegisterRequestCommand registerRequestCommand,
            RoleEnum roleName
    ) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(
                registerRequestCommand.username(), registerRequestCommand.email());
        if (existed)
            throw new ServiceException("Người dùng đã tồn tại");

        String rawPassword = decryptNewPassword(registerRequestCommand.password(), registerRequestCommand.confirmPassword());

        String hashedPassword = jwtPort.hashPassword(rawPassword);

        User user = new User();
        user.setUsername(registerRequestCommand.username());
        user.setPassword(hashedPassword);
        user.setFullName(registerRequestCommand.fullName());
        user.setEmail(registerRequestCommand.email());
        user.setStatus(UserStatusEnum.INACTIVE);
        user.getAuthProviders().add(AuthProviderEnum.LOCAL);

        Role role = roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
        user.setRole(role);

        User createdUser = userRepository.save(user);

        userRegistrationEventPublisher.publishUserRegistered(createdUser, rawPassword);

        String verificationCode = BaseUtil.generateVerificationCode();
        this.redisPort.saveWithTTL(
                createdUser.getUserId().toString(),
                verificationCode,
                jwtPort.getAuthProperties().verificationCodeValidityInSecond(),
                TimeUnit.SECONDS);

        emailNotificationPort.sendVerificationEmail(createdUser.getEmail(), verificationCode);

        return userMapper.toUserResponseCommand(createdUser);
    }

    /**
     * Cấp phiên mới: access + refresh token của GOAT Sports. Mỗi người chỉ giữ MỘT refresh token hợp lệ
     * (ghi đè cái cũ), nên đăng nhập ở thiết bị mới sẽ đăng xuất phiên refresh ở thiết bị cũ.
     */
    private LoginResponseCommand issueSession(User user, boolean accountRestored) {
        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);
        redisPort.deleteKey(refreshKey(user.getUserId().toString()));
        redisPort.saveWithTTL(
                refreshKey(user.getUserId().toString()),
                refreshToken,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(),
                TimeUnit.SECONDS);
        return new LoginResponseCommand(userMapper.toUserResponseCommand(user), accessToken, refreshToken,
                accountRestored);
    }

    /**
     * Mật khẩu đi trên đường truyền được mã hóa RSA bằng public key của server; giải mã rồi mới kiểm tra
     * độ mạnh và khớp xác nhận. Trả về mật khẩu gốc để băm.
     */
    private String decryptNewPassword(String encryptedPassword, String encryptedConfirm) throws ServiceException {
        String rawPassword = rsaPort.decrypt(encryptedPassword);
        String rawConfirmPassword = rsaPort.decrypt(encryptedConfirm);
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ServiceException("Mật khẩu không được để trống");
        }
        if (!PasswordPolicy.isStrong(rawPassword)) {
            throw new ServiceException(PasswordPolicy.STRENGTH_MESSAGE);
        }
        if (!rawPassword.equals(rawConfirmPassword)) {
            throw new ServiceException("Mật khẩu và xác nhận mật khẩu không khớp");
        }
        return rawPassword;
    }

    @Override
    public void changeUserStatus(UUID userId, UserStatusEnum status) throws ServiceException {
        if (status != UserStatusEnum.ACTIVE && status != UserStatusEnum.BLOCKED) {
            throw new ServiceException("Chỉ có thể khóa hoặc mở khóa tài khoản.");
        }
        String actorId = currentUserProvider.getCurrentUserId().orElse(null);
        if (userId.toString().equals(actorId)) {
            throw new ServiceException("Không thể tự khóa tài khoản của chính mình.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
        if (user.getStatus() == UserStatusEnum.PENDING_DELETION || user.getStatus() == UserStatusEnum.DELETED) {
            throw new ServiceException("Tài khoản đang chờ xóa hoặc đã xóa, không thể khóa hay mở khóa.");
        }
        if (status == UserStatusEnum.BLOCKED && user.getRole() != null && RoleEnum.ADMIN.name().equals(user.getRole().getName())) {
            throw new ServiceException("Không thể khóa tài khoản quản trị viên.");
        }
        if (status == UserStatusEnum.ACTIVE && user.getStatus() != UserStatusEnum.BLOCKED) {
            throw new ServiceException("Tài khoản không ở trạng thái bị khóa.");
        }
        user.setStatus(status);
        userRepository.save(user);
        if (status == UserStatusEnum.BLOCKED) {
            redisPort.saveWithTTL(blockedUserKey(userId), "1",
                    jwtPort.getAuthProperties().refreshTokenExpirationSecond(), TimeUnit.SECONDS);
            redisPort.deleteKey(refreshKey(userId));
            redisPort.deleteKey(keycloakRefreshKey(userId));
            sessionRevocationPort.notifySessionRevoked(userId, SessionRevocationPort.ACCOUNT_BLOCKED);
        } else {
            redisPort.deleteKey(blockedUserKey(userId));
        }
    }

    private static String blockedUserKey(Object userId) {
        return "blocked_user:" + userId;
    }

    private static String refreshKey(Object userId) {
        return "user:" + userId + ":refresh";
    }

    private static String keycloakRefreshKey(Object userId) {
        return "user:" + userId + ":keycloakRefresh";
    }
}
