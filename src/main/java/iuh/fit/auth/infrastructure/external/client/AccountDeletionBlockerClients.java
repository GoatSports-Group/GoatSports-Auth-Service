package iuh.fit.auth.infrastructure.external.client;

import iuh.fit.auth.application.dto.response.base.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/** Mỗi service tự trả lời "người dùng này còn việc gì dang dở ở chỗ tôi": cùng một đường dẫn nội bộ. */
public final class AccountDeletionBlockerClients {
    private AccountDeletionBlockerClients() {
    }

    static final String PATH = "/internal/users/{userId}/deletion-blockers";

    @FeignClient(name = "deletion-blockers-venue", url = "${venue.service.url}", configuration = StorageClientConfig.class)
    public interface Venue {
        @GetMapping(PATH)
        BaseResponse<List<String>> deletionBlockers(@PathVariable("userId") UUID userId);
    }

    @FeignClient(name = "deletion-blockers-payment", url = "${payment.service.url}", configuration = StorageClientConfig.class)
    public interface Payment {
        @GetMapping(PATH)
        BaseResponse<List<String>> deletionBlockers(@PathVariable("userId") UUID userId);
    }

    @FeignClient(name = "deletion-blockers-club", url = "${club.service.url}", configuration = StorageClientConfig.class)
    public interface Club {
        @GetMapping(PATH)
        BaseResponse<List<String>> deletionBlockers(@PathVariable("userId") UUID userId);
    }
}
