package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AccountDeletionService;
import iuh.fit.auth.application.service.impl.AccountDeletionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@org.springframework.context.annotation.Primary
@RequiredArgsConstructor
public class AccountDeletionDecorator implements AccountDeletionService {
    private final AccountDeletionServiceImpl accountDeletionServiceImpl;

    @Override
    public List<String> currentBlockers() throws ServiceException {
        return accountDeletionServiceImpl.currentBlockers();
    }

    @Override
    public void sendCode() throws ServiceException {
        accountDeletionServiceImpl.sendCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalDateTime requestDeletion(String code) throws ServiceException {
        return accountDeletionServiceImpl.requestDeletion(code);
    }

    @Override
    public List<UUID> findDueForPurge(int limit) {
        return accountDeletionServiceImpl.findDueForPurge(limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purge(UUID userId) throws ServiceException {
        accountDeletionServiceImpl.purge(userId);
    }
}
