package iuh.fit.auth.presentation.response.permission;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {
    private UUID permissionId;
    private String name;
    private String apiPath;
    private String method;
    private String module;
}
