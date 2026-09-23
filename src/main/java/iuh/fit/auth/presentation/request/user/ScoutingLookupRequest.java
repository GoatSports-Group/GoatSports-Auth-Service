package iuh.fit.auth.presentation.request.user;

import iuh.fit.auth.domain.enums.SportType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ScoutingLookupRequest(
        @NotNull SportType sportType,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotEmpty @Size(max = 200) List<@NotNull UUID> userIds
) { }
