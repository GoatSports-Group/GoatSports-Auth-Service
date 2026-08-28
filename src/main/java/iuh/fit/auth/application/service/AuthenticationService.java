package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.request.auth.*;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.auth.application.dto.response.auth.RefreshResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;

public interface AuthenticationService {
    UserResponseCommand register(RegisterRequestCommand registerRequestCommand) throws ServiceException;

    UserResponseCommand registerVenueOwner(RegisterRequestCommand registerRequestCommand) throws ServiceException;

    LoginResponseCommand login(LoginRequestCommand loginCommand) throws ServiceException;

    void verifyUser(VerificationRequestCommand verificationRequestCommand) throws ServiceException;

    void resendVerificationCode(String email) throws ServiceException;

    void logout(String accessToken, String refreshToken);

    RefreshResponseCommand refresh(String refreshToken) throws ServiceException;

    void forgotPassword(ForgotPasswordRequestCommand forgotPasswordRequestCommand) throws ServiceException;

    UserResponseCommand getCurrentUser() throws ServiceException;

    LoginResponseCommand callback(String code, String redirectUri) throws ServiceException;
}
