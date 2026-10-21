package iuh.fit.auth.infrastructure.external.adapter;

import iuh.fit.auth.application.dto.response.base.BaseResponse;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.client.AccountDeletionBlockerPort;
import iuh.fit.auth.infrastructure.external.client.AccountDeletionBlockerClients;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionBlockerAdapter implements AccountDeletionBlockerPort {
    private final AccountDeletionBlockerClients.Venue venue;
    private final AccountDeletionBlockerClients.Payment payment;
    private final AccountDeletionBlockerClients.Club club;

    @Override
    public List<String> findBlockers(UUID userId) throws ServiceException {
        List<String> blockers = new ArrayList<>();
        blockers.addAll(ask("venue-service", userId, venue::deletionBlockers));
        blockers.addAll(ask("payment-service", userId, payment::deletionBlockers));
        blockers.addAll(ask("club-service", userId, club::deletionBlockers));
        return blockers;
    }

    private static List<String> ask(String service, UUID userId, Function<UUID, BaseResponse<List<String>>> call)
            throws ServiceException {
        try {
            BaseResponse<List<String>> response = call.apply(userId);
            return response == null || response.getData() == null ? List.of() : response.getData();
        } catch (Exception e) {
            log.warn("Could not read deletion blockers from {} for user {}: {}", service, userId, e.getMessage());
            throw new ServiceException("Chưa kiểm tra được dữ liệu tài khoản, vui lòng thử lại sau.");
        }
    }
}
