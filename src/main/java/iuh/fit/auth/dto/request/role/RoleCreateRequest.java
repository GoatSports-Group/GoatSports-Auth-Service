package iuh.fit.auth.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateRequest {
    @NotBlank(message = "Tên vai trò không để trống")
    private String name;
    private String description;
}
