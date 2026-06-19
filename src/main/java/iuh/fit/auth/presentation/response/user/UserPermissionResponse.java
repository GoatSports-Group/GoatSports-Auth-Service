package iuh.fit.auth.presentation.response.user;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionResponse {
    private String email;
    private String role;
    private Set<String> permissions;
}
