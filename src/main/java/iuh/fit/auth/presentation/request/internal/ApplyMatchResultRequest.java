package iuh.fit.auth.presentation.request.internal;

import iuh.fit.auth.domain.enums.SportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplyMatchResultRequest(
        @NotNull UUID sessionId,
        @NotNull UUID userId,
        @NotNull SportType sportType,
        @NotNull @Min(0) @Max(5000) Integer opponentElo,
        @NotBlank String outcome
) {
}
