package iuh.fit.auth.presentation.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {
    @NotBlank(message = "User ID Không để trống")
    private String userId;
    @NotBlank(message = "Role ID Không để trống")
    private String roleId;
}
