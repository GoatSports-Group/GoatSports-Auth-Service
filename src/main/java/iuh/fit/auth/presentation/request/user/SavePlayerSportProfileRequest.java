package iuh.fit.auth.presentation.request.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavePlayerSportProfileRequest {
    @NotNull(message = "Môn thể thao không được để trống")
    private SportType sportType;

    @NotNull(message = "Trình độ không được để trống")
    private SkillLevel skillLevel;

    @Size(max = 10, message = "Tối đa 10 vị trí yêu thích")
    private Set<@Size(max = 50, message = "Vị trí không được vượt quá 50 ký tự") String> preferredPositions
            = new HashSet<>();

    @Size(max = 255, message = "Phong cách chơi không được vượt quá 255 ký tự")
    private String playStyle;

    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
    private Double longitude;

    @DecimalMin(value = "0.1", message = "Bán kính chơi phải lớn hơn 0")
    @DecimalMax(value = "200.0", message = "Bán kính chơi không được vượt quá 200 km")
    private Double playRadiusKm;

    @Valid
    private List<AvailabilityRequest> availabilities = new ArrayList<>();
}
