package iuh.fit.auth.presentation.request.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UserSummariesRequest(
        @NotNull(message = "Danh sách mã người dùng không được để trống")
        @Size(max = 100, message = "Chỉ tra cứu tối đa 100 người dùng mỗi lần")
        List<@NotNull UUID> userIds
) {}
