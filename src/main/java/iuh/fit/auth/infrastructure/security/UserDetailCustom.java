package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
@RequiredArgsConstructor
public class UserDetailCustom implements UserDetailsService {
    private final JpaUserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException, DisabledException {
        UserEntity user = userRepository.findByUsernameOrEmailWithRole(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));

        if(user.getPassword() == null) {
            throw new DisabledException("Tài khoản được đăng nhập bằng mạng xã hội");
        }

        if (user.getStatus() == UserStatusEnum.INACTIVE) {
            throw new DisabledException("Tài khoản chưa được kích hoạt hoặc đã bị khóa");
        }

        if (user.getRole() == null || !user.getRole().isActive()) {
            throw new DisabledException("vai trò của bạn không có quyền đăng nhập");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName().name()))
        );
    }
}
