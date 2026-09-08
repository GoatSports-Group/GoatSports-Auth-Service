package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.request.user.SavePlayerSportProfileCommand;
import iuh.fit.auth.application.dto.response.user.PlayerSportProfileResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface PlayerSportProfileService {
    List<PlayerSportProfileResponseCommand> getMyProfiles() throws ServiceException;

    PlayerSportProfileResponseCommand createMyProfile(SavePlayerSportProfileCommand command) throws ServiceException;

    PlayerSportProfileResponseCommand updateMyProfile(
            UUID profileId,
            SavePlayerSportProfileCommand command
    ) throws ServiceException;

    void deleteMyProfile(UUID profileId) throws ServiceException;
}
