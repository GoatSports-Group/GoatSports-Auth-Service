package iuh.fit.auth.infrastructure.external.adapter;

import iuh.fit.auth.application.dto.request.file.FileMoveRequest;
import iuh.fit.auth.application.dto.request.file.MoveFileRequest;
import iuh.fit.auth.application.dto.response.base.BaseResponse;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.client.StoragePort;
import iuh.fit.auth.infrastructure.external.client.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StorageAdapter implements StoragePort {
    private final StorageClient storageClient;

    @Override
    public List<String> confirmFile(List<FileMoveRequest> requests) throws ServiceException {
        BaseResponse<List<String>> response = storageClient.confirmFile(requests);

        if(response.getError() != null) {
            throw new ServiceException("Xác nhận tài liệu thất bại: " + response.getMessage());
        }

        return response.getData();
    }

    @Override
    public void moveFile(MoveFileRequest request) throws ServiceException {
        BaseResponse<Void> response = storageClient.moveFile(request);

        if(response.getError() != null) {
            throw new ServiceException("Di chuyển tài liệu thất bại: " + response.getMessage());
        }
    }

    @Override
    public String getFileUrl(String key) throws ServiceException {
        String avatarUrl = storageClient.getFileUrl(key);
        if(avatarUrl == null || avatarUrl.isEmpty()) {
            throw new ServiceException("Lấy ảnh thất bại");
        }

        return avatarUrl;
    }
}
