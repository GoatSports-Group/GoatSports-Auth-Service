package iuh.fit.auth.dto.request.role;

import lombok.*;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    @NotBlank(message = "Mã vai trò không để trống")
    private String roleId;
    @NotBlank(message = "Tên vai trò không để trống")
    private String name;
    private String description;
    private boolean active;
    @NotNull(message = "Danh sách quyền không để trống")
    private Set<@NotBlank(message = "Mã quyền không được để trống") String> permissionIds;
}
