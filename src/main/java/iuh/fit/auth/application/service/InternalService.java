package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InternalService {
    UserPermissionsResponseCommand getUserPermissions(String userId) throws ServiceException;

    void assignVenueOwnerRole(String userId) throws ServiceException;

    String getAdmin() throws ServiceException;

    List<UserReportResponseCommand> getAllUserReports();

    UserResponseCommand getUserById(String userId) throws ServiceException;

    List<UUID> findRecipientIds(Set<String> roleNames, UUID after, int limit) throws ServiceException;

    long countRecipients(Set<String> roleNames) throws ServiceException;

    /** venue-service gọi khi người chơi cọc xong: gửi vé QR tới email tài khoản (thư đi bất đồng bộ). */
    void sendBookingTicket(UUID userId, EmailNotificationPort.BookingTicketMail ticket) throws ServiceException;
}
