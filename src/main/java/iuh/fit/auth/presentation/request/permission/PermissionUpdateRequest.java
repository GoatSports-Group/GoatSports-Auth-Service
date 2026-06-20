package iuh.fit.auth.presentation.request.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionUpdateRequest {
    @NotBlank(message = "Mã quyền không để trống")
    private String permissionId;
    @NotBlank(message = "Tên quyền không để trống")
    private String name;
    @NotBlank(message = "Đường dẫn API không để trống")
    private String apiPath;
    @NotBlank(message = "Phương thức HTTP không để trống")
    private String method;
    @NotBlank(message = "Module không để trống")
    private String module;
}
