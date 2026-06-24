package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;

public interface InternalService {
    UserPermissionsResponseCommand getUserPermissions(String userId) throws ServiceException;

    void assignVenueOwnerRole(String userId) throws ServiceException;
}
