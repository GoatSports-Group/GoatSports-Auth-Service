package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameOrEmailWithRole(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new DisabledException(
                    "Tài khoản này đăng nhập bằng bên thứ 3. Vui lòng quên mật khẩu để đặt mật khẩu."
            );
        }

        if (user.getRole() == null || !user.getRole().isActive()) {
            throw new DisabledException("vai trò của bạn không có quyền đăng nhập");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()))
        );
    }
}
