package iuh.fit.auth.presentation.response.role;

import iuh.fit.auth.presentation.response.permission.PermissionResponse;
import lombok.*;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private UUID roleId;
    private String name;
    private String description;
    private boolean active;
    private Set<PermissionResponse> permissions;
}
