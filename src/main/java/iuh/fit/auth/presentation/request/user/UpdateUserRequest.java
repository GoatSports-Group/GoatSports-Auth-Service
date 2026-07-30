package iuh.fit.auth.presentation.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    private String phone;
    private String country;
    @NotBlank(message = "Giới tính không được để trống")
    private String gender;
}
