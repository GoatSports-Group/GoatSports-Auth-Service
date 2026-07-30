package iuh.fit.auth.application.port.client;

import iuh.fit.auth.application.dto.request.file.FileMoveRequest;
import iuh.fit.auth.application.dto.request.file.MoveFileRequest;
import iuh.fit.auth.application.exception.ServiceException;

import java.util.List;

public interface StoragePort {
    List<String> confirmFile(List<FileMoveRequest> requests) throws ServiceException;

    void moveFile(MoveFileRequest request) throws ServiceException;

    String getFileUrl(String key) throws ServiceException;
}
