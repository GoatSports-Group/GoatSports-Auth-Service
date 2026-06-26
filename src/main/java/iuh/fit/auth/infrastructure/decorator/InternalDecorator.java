package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.InternalService;
import iuh.fit.auth.application.service.impl.InternalServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalDecorator implements InternalService {
    private final InternalServiceImpl internalServiceImpl;

    @Override
    @Transactional(readOnly = true)
    public UserPermissionsResponseCommand getUserPermissions(String userId) throws ServiceException {
        return internalServiceImpl.getUserPermissions(userId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void assignVenueOwnerRole(String userId) throws ServiceException {
        internalServiceImpl.assignVenueOwnerRole(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAdmin() throws ServiceException {
        return internalServiceImpl.getAdmin();
    }
}
