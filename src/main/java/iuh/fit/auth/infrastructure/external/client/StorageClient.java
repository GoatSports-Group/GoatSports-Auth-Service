package iuh.fit.auth.infrastructure.external.client;

import iuh.fit.auth.application.dto.request.file.FileMoveRequest;
import iuh.fit.auth.application.dto.request.file.MoveFileRequest;
import iuh.fit.auth.application.dto.response.base.BaseResponse;
import iuh.fit.auth.application.exception.ServiceException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "GOATSPORTS-STORAGE-SERVICE",
    url = "${storage.service.url}",
    path = "/internal/files"
)
public interface StorageClient {
    @PostMapping("/confirm")
    BaseResponse<List<String>> confirmFile(@RequestBody List<FileMoveRequest> requests);

    @PostMapping("/move")
    BaseResponse<Void> moveFile(@RequestBody MoveFileRequest request);

    @GetMapping
    String getFileUrl(@RequestParam("key") String key) throws ServiceException;
}
