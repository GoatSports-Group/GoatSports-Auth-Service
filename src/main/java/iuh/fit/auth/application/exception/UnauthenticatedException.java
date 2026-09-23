package iuh.fit.auth.application.exception;

public class UnauthenticatedException extends ServiceException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
