package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.AppliedMatchResult;

public interface MatchResultService {
    AppliedMatchResult apply(ApplyMatchResultCommand command) throws ServiceException;
}
