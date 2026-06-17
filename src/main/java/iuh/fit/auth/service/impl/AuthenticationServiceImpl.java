package iuh.fit.auth.service.impl;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import iuh.fit.auth.dto.request.authentication.*;
import iuh.fit.auth.dto.response.authentication.KeycloakResponse;
import iuh.fit.auth.enums.AuthProvider;
import iuh.fit.auth.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.enums.RoleEnum;
import iuh.fit.auth.enums.UserStatusEnum;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.repository.RoleRepository;
import iuh.fit.auth.repository.UserRepository;
import iuh.fit.auth.util.BaseUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
        private final JwtDecoder keycloakJwtDecoder;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManagerBuilder authenticationManagerBuilder;

        private final KeycloakService keycloakService;
        private final JwtService jwtService;
        private final RedisService redisService;
        private final EmailNotificationService emailNotificationService;

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;

        @Value("${app.verify-code-validity-in-second}")
        private int verificationCodeValidityInSecond;
        @Value("${app.jwt.refresh-token.expiration-second}")
        private long refreshTokenExpirationSecond;
        @Value("${app.jwt.access-token.expiration-second}")
        private long accessTokenExpirationSecond;
        @Value("${app.cookie.domain}")
        private String cookieDomain;
        @Value("${app.cookie.secure}")
        private boolean cookieSecure;

        @Override
        public UserResponse login(
                LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request
        ) throws ServiceException
        {
                Authentication authentication = authenticationManagerBuilder.getObject()
                        .authenticate(
                                new UsernamePasswordAuthenticationToken(
                                        loginRequest.getUsername(),
                                        loginRequest.getPassword()
                                )
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                User user = userRepository.findByUsernameOrEmailWithRole(loginRequest.getUsername(), loginRequest.getUsername())
                        .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

                UserResponse userResponse = convertToResponse(user);

                String oldRefreshToken = this.redisService.getValue("user:" + user.getUserId() + ":refresh");
                if (oldRefreshToken != null) {
                        this.redisService.deleteKey("user:" + user.getUserId() + ":refresh");
                }

                String accessToken = jwtService.generateAccessToken(userResponse);
                String refreshToken = jwtService.generateRefreshToken(userResponse);

                this.redisService.saveWithTTL(
                                "user:" + user.getUserId() + ":refresh",
                                refreshToken,
                                jwtService.getRefreshTokenExpirationSecond(),
                                TimeUnit.SECONDS);

                ResponseCookie accessCookie = createCookie("accessToken", accessToken, jwtService.getAccessTokenExpirationSecond());
                ResponseCookie refreshCookie = createCookie("refreshToken", refreshToken, jwtService.getRefreshTokenExpirationSecond());

                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                return userResponse;
        }

        @Override
        public UserResponse loginWithKeycloak(LinkAccountRequest request, HttpServletResponse response) throws ServiceException {
                KeycloakResponse token = keycloakService.exchangeCode(request.getCode(), request.getRedirectUri());

                Jwt jwt = keycloakJwtDecoder.decode(token.getAccessToken());

                UUID keycloakId = UUID.fromString(jwt.getSubject());
                String email = jwt.getClaimAsString("email");
                String username = jwt.getClaimAsString("preferred_username");
                String fullName = jwt.getClaimAsString("name");

                Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                        .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));

                User user = userRepository.findByKeycloakId(keycloakId)
                        .orElseGet(() -> {
                                User newUser = new User();
                                newUser.setKeycloakId(keycloakId);
                                newUser.setEmail(email);
                                newUser.setUsername(username);
                                newUser.setFullName(fullName);
                                newUser.setRole(role);
                                newUser.setStatus(UserStatusEnum.ACTIVE.name());
                                newUser.setAuthProvider(AuthProvider.KEYCLOAK);
                                return userRepository.save(newUser);
                        });

                UserResponse userResponse = convertToResponse(user);

                String accessToken = jwtService.generateAccessToken(userResponse);
                String refreshToken = jwtService.generateRefreshToken(userResponse);

                this.redisService.saveWithTTL(
                        "user:" + user.getUserId() + ":refresh",
                        refreshToken,
                        jwtService.getRefreshTokenExpirationSecond(),
                        TimeUnit.SECONDS);

                this.redisService.saveWithTTL(
                        "user:" + user.getUserId() + ":keycloakRefresh",
                        token.getRefreshToken(),
                        token.getRefreshExpiresIn(),
                        TimeUnit.SECONDS);

                ResponseCookie accessCookie = createCookie("accessToken", accessToken, jwtService.getAccessTokenExpirationSecond());
                ResponseCookie refreshCookie = createCookie("refreshToken", refreshToken, jwtService.getRefreshTokenExpirationSecond());

                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                return userResponse;
        }

        @Override
        @Transactional
        public void linkKeycloakAccount(String code, String redirectUri) throws ServiceException {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null) {
                        throw new ServiceException("Bạn cần đăng nhập trước khi liên kết tài khoản");
                }

                User user = getCurrentUser();
                if(user.getKeycloakId() != null) {
                        throw new ServiceException("Tài khoản đã được liên kết với Keycloak");
                }

                KeycloakResponse token = keycloakService.exchangeCode(code, redirectUri);
                Jwt jwt = keycloakJwtDecoder.decode(token.getAccessToken());
                String keycloakIdStr = jwt.getSubject();
                if(keycloakIdStr == null || keycloakIdStr.isEmpty() ) {
                        keycloakService.logout(token.getRefreshToken());
                        throw new ServiceException("Không thể lấy thông tin từ Keycloak");
                }

                UUID keycloakId = UUID.fromString(keycloakIdStr);
                boolean checkedKeycloak = userRepository.existsByKeycloakId(keycloakId);
                if(checkedKeycloak) {
                        keycloakService.logout(token.getRefreshToken());
                        throw new ServiceException("Tài khoản Keycloak đã được liên kết với tài khoản khác");
                }

                user.setKeycloakId(keycloakId);
                user.setAuthProvider(AuthProvider.ALL);
                userRepository.save(user);

                this.redisService.saveWithTTL(
                        "user:" + user.getUserId() + ":keycloakRefresh",
                        token.getRefreshToken(),
                        token.getRefreshExpiresIn(),
                        TimeUnit.SECONDS);
        }

        @Override
        public UserResponse register(RegisterRequest request) throws ServiceException {
                boolean existed = userRepository.existsByUsernameOrEmail(request.getUsername(), request.getEmail());
                if (existed)
                        throw new ServiceException("Người dùng đã tồn tại");

                String hashedPassword = this.passwordEncoder.encode(request.getPassword());

                User user = new User();
                user.setUsername(request.getUsername());
                user.setPassword(hashedPassword);
                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setStatus(UserStatusEnum.INACTIVE.name());
                user.setAuthProvider(AuthProvider.LOCAL);

                Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                        .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
                user.setRole(role);

                User createdUser = userRepository.save(user);

                String verificationCode = BaseUtil.generateVerificationCode();
                this.redisService.saveWithTTL(
                        createdUser.getUserId().toString(),
                        verificationCode,
                        verificationCodeValidityInSecond,
                        TimeUnit.SECONDS);

                emailNotificationService.sendVerificationEmail(createdUser.getEmail(), verificationCode);

                return convertToResponse(createdUser);
        }

        @Override
        public void verifyUser(VerificationRequest verificationRequest) throws ServiceException {
                User user = userRepository.findByEmail(verificationRequest.getEmail())
                                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

                if (!this.redisService.hasKey(user.getUserId().toString())) {
                        throw new ServiceException("Mã xác thực đã hết hạn");
                }
                if (!this.redisService.getValue(user.getUserId().toString())
                                .equalsIgnoreCase(verificationRequest.getVerificationCode())) {
                        throw new ServiceException("Mã xác thực không chính xác");
                }

                if(user.getAuthProvider() != null && user.getAuthProvider() == AuthProvider.KEYCLOAK) {
                        user.setAuthProvider(AuthProvider.ALL);
                }

                user.setStatus(UserStatusEnum.ACTIVE.name());
                this.userRepository.save(user);

                this.redisService.deleteKey(user.getUserId().toString());
        }

        @Override
        public void resendVerificationCode(String email) throws ServiceException {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null)
                        throw new ServiceException("Người dùng không tồn tại");

                String key = user.getUserId().toString();
                String verificationCode = BaseUtil.generateVerificationCode();
                this.redisService.replaceKey(
                                key,
                                key,
                                verificationCode,
                                verificationCodeValidityInSecond,
                                TimeUnit.SECONDS);
                emailNotificationService.sendVerificationEmail(email, verificationCode);

        }

        @Override
        public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
                Jwt jwt = jwtService.checkValidToken(refreshToken);
                String userId = jwt.getSubject();
                if (userId == null || userId.isEmpty())
                        return;

                redisService.deleteKey("user:" + userId + ":refresh");
                redisService.saveWithTTL(
                                "blacklist:" + accessToken,
                                "revoked",
                                jwtService.getRemainingTime(accessToken),
                                TimeUnit.SECONDS

                );

                if(redisService.hasKey("user:" + userId + ":keycloakRefresh")) {
                        String keycloakRefresh = redisService.getValue("user:" + userId + ":keycloakRefresh");
                        keycloakService.logout(keycloakRefresh);
                        redisService.deleteKey("user:" + userId + ":keycloakRefresh");
                }

                ResponseCookie deleteAccessCookie = createCookie("accessToken", "", 0);
                ResponseCookie deleteRefreshCookie = createCookie("refreshToken", "", 0);

                response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());
        }

        @Override
        public UserResponse refresh(String refreshToken, HttpServletResponse response) throws ServiceException {
                Jwt jwt = jwtService.checkValidToken(refreshToken);
                String userId = jwt.getSubject();
                String role = jwt.getClaimAsString("role");

                if (refreshToken.equalsIgnoreCase("missingValue")) {
                        throw new ServiceException("Hết hạn đăng nhập");
                }
                if (!this.redisService.hasKey("user:" + userId + ":refresh")
                                || !this.redisService.getValue("user:" + userId + ":refresh")
                                                .equalsIgnoreCase(refreshToken)) {
                        throw new ServiceException("Refresh token hết hạn");
                }

                User currentUser = userRepository.findById(UUID.fromString(userId)).orElse(null);
                if (currentUser == null)
                        throw new ServiceException("Người dùng không tồn tại");
                if (currentUser.getStatus().equals(UserStatusEnum.INACTIVE.name()))
                        throw new ServiceException("Người dùng bị khóa");

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        currentUser, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserResponse userResponse = convertToResponse(currentUser);

                String newAccessToken = jwtService.generateAccessToken(userResponse);
                String newRefreshToken = jwtService.generateRefreshToken(userResponse);

                this.redisService.replaceKey(
                                "user:" + userId + ":refresh",
                                "user:" + userId + ":refresh",
                                newRefreshToken,
                                refreshTokenExpirationSecond,
                                TimeUnit.SECONDS);

                ResponseCookie newAccessCookie = createCookie("accessToken", newAccessToken, accessTokenExpirationSecond);
                ResponseCookie newRefreshCookie = createCookie("refreshToken", newRefreshToken, refreshTokenExpirationSecond);

                response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

                return userResponse;
        }

        @Override
        public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws ServiceException {
                User user = userRepository.findByEmail(forgotPasswordRequest.getEmail()).orElse(null);

                if (user == null)
                        throw new ServiceException("Người dùng không tồn tại");
                if (user.getStatus().equals(UserStatusEnum.INACTIVE.name()))
                        throw new ServiceException("Người dùng bị khóa");

                String hashedPassword = this.passwordEncoder.encode(forgotPasswordRequest.getPassword());
                user.setPassword(hashedPassword);
                user.setStatus(UserStatusEnum.INACTIVE.name());
                this.userRepository.save(user);

                String verificationCode = BaseUtil.generateVerificationCode();
                this.redisService.saveWithTTL(
                                user.getUserId().toString(),
                                verificationCode,
                                verificationCodeValidityInSecond,
                                TimeUnit.SECONDS);
                this.emailNotificationService.sendVerificationEmail(user.getEmail(), verificationCode);
        }

        @Override
        public User getCurrentUser() throws ServiceException {
                String springUser = JwtService.getCurrentUser().isPresent()
                                ? JwtService.getCurrentUser().get()
                                : null;

                if (springUser == null || springUser.isEmpty()) {
                        throw new ServiceException("Phiên đăng nhập hết hạn");
                }

                User user;
                if(isUUID(springUser)) {
                        user = userRepository.findById(UUID.fromString(springUser))
                                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
                } else {
                        user = userRepository.findByUsernameOrEmailWithRole(springUser, springUser)
                                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
                }
                
                return user;
        }

        @Override
        public UserResponse convertToResponse(User user) {
            return UserResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .status(user.getStatus())
                            .createdAt(user.getCreatedAt())
                            .updatedAt(user.getUpdatedAt())
                            .role(new UserResponse.UserRole(
                                            user.getRole().getRoleId(),
                                            user.getRole().getName()))
                            .keycloakId(user.getKeycloakId())
                            .authProvider(user.getAuthProvider() != null ? user.getAuthProvider() : null)
                            .build();
        }

        private ResponseCookie createCookie(String name, String value, long maxAge) {
                ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .sameSite("Lax")
                                .path("/")
                                .maxAge(maxAge);
//                if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
//                        builder.domain(cookieDomain);
//                }
                return builder.build();
        }

        private boolean isUUID(String uuid) {
                try {
                        UUID.fromString(uuid);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }
}
