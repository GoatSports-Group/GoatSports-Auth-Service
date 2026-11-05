package iuh.fit.auth.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextHelperTest {
    private final SecurityContextHelper helper = new SecurityContextHelper();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** Khách xem hồ sơ công khai: "anonymousUser" không được trùng với tài khoản nào có username đó. */
    @Test
    void guestHasNoUsername() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(helper.getCurrentUser()).isEmpty();
    }

    @Test
    void signedInUserKeepsTheirUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("player@example.com", null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(helper.getCurrentUser()).contains("player@example.com");
    }
}
