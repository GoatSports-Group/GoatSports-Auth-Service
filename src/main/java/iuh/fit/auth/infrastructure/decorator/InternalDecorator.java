package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.service.InternalService;
import iuh.fit.auth.application.service.impl.InternalServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Set;

@Service
@Primary
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

    @Override
    @Transactional(readOnly = true)
    public List<UserReportResponseCommand> getAllUserReports() {
        return internalServiceImpl.getAllUserReports();
    }

    @Override
    public UserResponseCommand getUserById(String userId) throws ServiceException {
        return internalServiceImpl.getUserById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findRecipientIds(Set<String> roleNames, UUID after, int limit) throws ServiceException {
        return internalServiceImpl.findRecipientIds(roleNames, after, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecipients(Set<String> roleNames) throws ServiceException {
        return internalServiceImpl.countRecipients(roleNames);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendBookingTicket(UUID userId, EmailNotificationPort.BookingTicketMail ticket) throws ServiceException {
        internalServiceImpl.sendBookingTicket(userId, ticket);
    }
}
