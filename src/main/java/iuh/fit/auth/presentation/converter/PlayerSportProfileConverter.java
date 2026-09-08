package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.response.user.AvailabilityResponseCommand;
import iuh.fit.auth.application.dto.response.user.PlayerSportProfileResponseCommand;
import iuh.fit.auth.presentation.response.user.AvailabilityResponse;
import iuh.fit.auth.presentation.response.user.PlayerSportProfileResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlayerSportProfileConverter {
    AvailabilityResponse toResponse(AvailabilityResponseCommand command);

    PlayerSportProfileResponse toResponse(PlayerSportProfileResponseCommand command);

    List<PlayerSportProfileResponse> toResponse(List<PlayerSportProfileResponseCommand> commands);
}
