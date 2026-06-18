package iuh.fit.auth.controller.admin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.entity.Permission;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.service.PermissionService;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody Permission permission)
            throws ServiceException {
        if (permissionService.existsPermission(permission)) {
            throw new ServiceException("Quyền hạn đã tồn tại");
        }
        Permission res = permissionService.createPermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody Permission permission)
            throws ServiceException {
        if (permissionService.getPermissionById(permission.getPermissionId().toString()) == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        Permission res = permissionService.updatePermission(permission);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable("id") String id) throws ServiceException {
        if (permissionService.getPermissionById(id) == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        permissionService.deletePermission(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable("id") String id) throws ServiceException {
        Permission permission = permissionService.getPermissionById(id);
        if (permission == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(permission);
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllPermissions(
            @Filter Specification<Permission> spec, Pageable pageable) {
        BaseListResponse result = permissionService.getAllPermissions(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
