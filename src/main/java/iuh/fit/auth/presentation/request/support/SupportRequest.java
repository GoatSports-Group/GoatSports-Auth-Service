package iuh.fit.auth.presentation.request.support;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Yeu cau tu trang "Lien he ho tro" (khach chua dang nhap cung gui duoc). */
@Getter
@Setter
public class SupportRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 120, message = "Họ và tên từ 2 đến 120 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 160, message = "Email quá dài")
    private String email;

    /** Chu de co dinh de hop thu ho tro loc nhanh. */
    @NotBlank(message = "Vui lòng chọn chủ đề")
    @Pattern(regexp = "BOOKING|REFUND|CLUB_TOURNAMENT|ACCOUNT|FEEDBACK", message = "Chủ đề không hợp lệ")
    private String topic;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 5, max = 160, message = "Tiêu đề từ 5 đến 160 ký tự")
    private String subject;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 10, max = 4000, message = "Nội dung từ 10 đến 4000 ký tự")
    private String message;
}
