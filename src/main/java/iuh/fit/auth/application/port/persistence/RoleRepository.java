package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.application.dto.response.base.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Optional<Role> findById(UUID id);

    Optional<Role> findByName(String name);

    Role save(Role role) throws ServiceException;

    void delete(Role role);

    PageResult<Role> findAll(PageFilter filter);
}
