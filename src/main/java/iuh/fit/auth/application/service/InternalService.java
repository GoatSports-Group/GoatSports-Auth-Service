package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;

import java.util.List;

public interface InternalService {
    UserPermissionsResponseCommand getUserPermissions(String userId) throws ServiceException;

    void assignVenueOwnerRole(String userId) throws ServiceException;

    String getAdmin() throws ServiceException;

    List<UserReportResponseCommand> getAllUserReports();

    UserResponseCommand getUserById(String userId) throws ServiceException;
}
