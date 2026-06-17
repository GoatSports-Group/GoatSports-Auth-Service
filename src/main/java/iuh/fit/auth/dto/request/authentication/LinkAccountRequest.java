package iuh.fit.auth.dto.request.authentication;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkAccountRequest {
    @NotBlank(message = "Code không được để trống")
    private String code;
    @NotBlank(message = "Đường dẫn không được để trống")
    private String redirectUri;
}
