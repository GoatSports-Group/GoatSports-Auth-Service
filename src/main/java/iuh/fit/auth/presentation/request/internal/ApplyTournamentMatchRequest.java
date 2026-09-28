package iuh.fit.auth.presentation.request.internal;

import iuh.fit.auth.domain.enums.SportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** club-service gửi khi một trận giải đấu có (hoặc đổi) tỉ số. */
public record ApplyTournamentMatchRequest(
        @NotNull UUID matchId,
        @NotNull UUID tournamentId,
        @NotNull SportType sportType,
        @Size(max = 255) String title,
        @NotNull @Valid Side first,
        @NotNull @Valid Side second
) {
    public record Side(
            @Size(max = 255) String name,
            @NotNull @Min(0) Integer score,
            @NotEmpty @Size(max = 30) List<@NotNull UUID> playerIds
    ) { }
}
