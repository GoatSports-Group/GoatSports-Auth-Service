package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.usecase.permission.PermissionUseCase;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class PermissionController {
    private final PermissionUseCase permissionUseCase;

    @PostMapping
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody Permission permission)
            throws ServiceException {
        if (permissionUseCase.existsPermission(permission)) {
            throw new ServiceException("Quyền hạn đã tồn tại");
        }
        Permission res = permissionUseCase.createPermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody Permission permission)
            throws ServiceException {
        if (permissionUseCase.getPermissionById(permission.getPermissionId().toString()) == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        Permission res = permissionUseCase.updatePermission(permission);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable("id") String id) throws ServiceException {
        if (permissionUseCase.getPermissionById(id) == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        permissionUseCase.deletePermission(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable("id") String id) throws ServiceException {
        Permission permission = permissionUseCase.getPermissionById(id);
        if (permission == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(permission);
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllPermissions(
            @Filter Specification<PermissionEntity> spec, Pageable pageable) {
        BaseListResponse result = permissionUseCase.getAllPermissions(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
