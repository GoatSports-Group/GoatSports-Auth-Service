package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.auth.*;
import iuh.fit.auth.application.dto.response.auth.KeycloakParseCommand;
import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
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

    public AuthenticationServiceImpl(
            JwtPort jwtPort, RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort, CurrentUserProvider currentUserProvider,
            UserRegistrationEventPublisher userRegistrationEventPublisher,
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper
    ) {
        this.jwtPort = jwtPort;
        this.redisPort = redisPort;
        this.keycloakPort = keycloakPort;
        this.emailNotificationPort = emailNotificationPort;
        this.currentUserProvider = currentUserProvider;
        this.userRegistrationEventPublisher = userRegistrationEventPublisher;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponseCommand register(RegisterRequestCommand registerRequestCommand) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(
                registerRequestCommand.username(), registerRequestCommand.username());
        if (existed)
            throw new ServiceException("Người dùng đã tồn tại");

        String hashedPassword = jwtPort.hashPassword(registerRequestCommand.password());

        User user = new User();
        user.setUsername(registerRequestCommand.username());
        user.setPassword(hashedPassword);
        user.setFullName(registerRequestCommand.fullName());
        user.setEmail(registerRequestCommand.email());
        user.setStatus(UserStatusEnum.INACTIVE);
        user.setGender(GenderEnum.valueOf(registerRequestCommand.gender()));
        user.getAuthProviders().add(AuthProviderEnum.LOCAL);

        Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
        user.setRole(role);

        User createdUser = userRepository.save(user);

        userRegistrationEventPublisher.publishUserRegistered(createdUser, registerRequestCommand.password());

        String verificationCode = BaseUtil.generateVerificationCode();
        this.redisPort.saveWithTTL(
                createdUser.getUserId().toString(),
                verificationCode,
                jwtPort.getAuthProperties().verificationCodeValidityInSecond(),
                TimeUnit.SECONDS);

        emailNotificationPort.sendVerificationEmail(createdUser.getEmail(), verificationCode);

        return userMapper.toUserResponseCommand(createdUser);
    }

    @Override
    public LoginResponseCommand login(LoginRequestCommand loginCommand) throws ServiceException {
        User user = userRepository.findByUsernameOrEmailWithRole(loginCommand.username(), loginCommand.username())
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        String oldRefreshToken = redisPort.getValue("user:" + user.getUserId() + ":refresh");
        if (oldRefreshToken != null) {
            redisPort.deleteKey("user:" + user.getUserId() + ":refresh");
        }

        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":refresh",
                refreshToken,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(),
                TimeUnit.SECONDS
        );

        return new LoginResponseCommand(userMapper.toUserResponseCommand(user), accessToken, refreshToken);
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
                TimeUnit.SECONDS
        );

        emailNotificationPort.sendVerificationEmail(email, verificationCode);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        String userId = jwtPort.getUserIdFromToken(refreshToken);
        if (userId == null || userId.isEmpty())
            return;

        redisPort.deleteKey("user:" + userId + ":refresh");
        redisPort.saveWithTTL(
                "blacklist:" + accessToken,
                "revoked",
                jwtPort.getRemainingTime(accessToken),
                TimeUnit.SECONDS
        );

        if (redisPort.hasKey("user:" + userId + ":keycloakRefresh")) {
            String keycloakRefresh = redisPort.getValue("user:" + userId + ":keycloakRefresh");
            try {
                keycloakPort.logout(keycloakRefresh);
            } catch (Exception e) {
                log.warn("Keycloak logout failed or unreachable: {}. Proceeding with local logout.", e.getMessage());
            }
            redisPort.deleteKey("user:" + userId + ":keycloakRefresh");
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

        String refreshKey = "user:" + userId + ":refresh";
        String keycloakRefreshKey = "user:" + userId + ":keycloakRefresh";

        String blacklistKey = "blacklist_refresh:" + refreshToken;
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
                            TimeUnit.SECONDS
                    );
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
        if (currentUser.getStatus() == UserStatusEnum.INACTIVE)
            throw new ServiceException("Người dùng bị khóa");

        String newAccessToken = jwtPort.generateAccessToken(currentUser);
        String newRefreshToken = jwtPort.generateRefreshToken(currentUser);

        this.redisPort.replaceKey(
                "user:" + userId + ":refresh",
                "user:" + userId + ":refresh",
                newRefreshToken,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(),
                TimeUnit.SECONDS
        );

        long remainingTime = jwtPort.getRemainingTime(refreshToken);
        if (remainingTime > 0) {
            redisPort.saveWithTTL(
                    blacklistKey,
                    "used",
                    remainingTime,
                    TimeUnit.SECONDS
            );
        }

        return new RefreshResponseCommand(userMapper.toUserResponseCommand(currentUser), role, newAccessToken, newRefreshToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestCommand forgotPasswordRequestCommand) throws ServiceException {
        User user = userRepository.findByEmail(forgotPasswordRequestCommand.email()).orElse(null);

        if (user == null)
            throw new ServiceException("Người dùng không tồn tại");
        if (user.getStatus() == UserStatusEnum.INACTIVE)
            throw new ServiceException("Người dùng bị khóa");

        String hashedPassword = jwtPort.hashPassword(forgotPasswordRequestCommand.password());
        user.setPassword(hashedPassword);
        user.setStatus(UserStatusEnum.INACTIVE);
        User savedUser = this.userRepository.save(user);

        userRegistrationEventPublisher.publishUserPasswordUpdated(savedUser, forgotPasswordRequestCommand.password());

        String verificationCode = BaseUtil.generateVerificationCode();
        redisPort.saveWithTTL(
                savedUser.getUserId().toString(),
                verificationCode,
                jwtPort.getAuthProperties().verificationCodeValidityInSecond(),
                TimeUnit.SECONDS
        );

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
                        username, email
                ).orElse(null));

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

         String oldKeycloakRefresh = redisPort.getValue("user:" + user.getUserId() + ":keycloakRefresh");
         if (oldKeycloakRefresh != null) {
             redisPort.deleteKey("user:" + user.getUserId() + ":keycloakRefresh");
         }
        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":keycloakRefresh",
                keycloakResponse.refreshToken(),
                keycloakResponse.refreshExpiresIn(),
                TimeUnit.SECONDS
        );

        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        redisPort.saveWithTTL(
                "user:" + user.getUserId() + ":refresh",
                refreshToken,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(),
                TimeUnit.SECONDS
        );

        return new LoginResponseCommand(userMapper.toUserResponseCommand(user), accessToken, refreshToken);
    }
}