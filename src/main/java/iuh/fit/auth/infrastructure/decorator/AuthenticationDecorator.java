package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.request.auth.*;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.auth.application.dto.response.auth.RefreshResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.application.service.impl.AuthenticationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationDecorator implements AuthenticationService {
    private final AuthenticationServiceImpl authenticationServiceImpl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseCommand login(LoginRequestCommand loginCommand) throws ServiceException {
        return authenticationServiceImpl.login(loginCommand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponseCommand register(RegisterRequestCommand registerRequestCommand) throws ServiceException {
        return authenticationServiceImpl.register(registerRequestCommand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponseCommand registerVenueOwner(RegisterRequestCommand registerRequestCommand) throws ServiceException {
        return authenticationServiceImpl.registerVenueOwner(registerRequestCommand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyUser(VerificationRequestCommand verificationRequestCommand) throws ServiceException {
        authenticationServiceImpl.verifyUser(verificationRequestCommand);
    }

    @Override
    public void resendVerificationCode(String email) throws ServiceException {
        authenticationServiceImpl.resendVerificationCode(email);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        authenticationServiceImpl.logout(accessToken, refreshToken);
    }

    @Override
    public RefreshResponseCommand refresh(String refreshToken) throws ServiceException {
        return authenticationServiceImpl.refresh(refreshToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestCommand forgotPasswordRequestCommand) throws ServiceException {
        authenticationServiceImpl.forgotPassword(forgotPasswordRequestCommand);
    }

    @Override
    public UserResponseCommand getCurrentUser() throws ServiceException {
        return authenticationServiceImpl.getCurrentUser();
    }

    @Override
    public LoginResponseCommand callback(String code, String redirectUri) throws ServiceException {
        return authenticationServiceImpl.callback(code, redirectUri);
    }
}
