package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.usecase.role.RoleUseCase;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.presentation.request.role.RoleCreateRequest;
import iuh.fit.auth.presentation.request.role.RoleUpdateRequest;
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
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleUseCase roleUseCase;

    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleCreateRequest role) {
        Role res = roleUseCase.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Role> updateRole(@Valid @RequestBody RoleUpdateRequest role) throws ServiceException {
        Role res = roleUseCase.updateRole(role);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) throws ServiceException {
        roleUseCase.deleteRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Role> activateRole(@PathVariable("id") String id) throws ServiceException {
        Role res = roleUseCase.activateRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Role> deactivateRole(@PathVariable("id") String id) throws ServiceException {
        Role res = roleUseCase.deactivateRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable("id") String id) throws ServiceException {
        Role role = roleUseCase.getRoleById(id);
        return ResponseEntity.status(HttpStatus.OK).body(role);
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllRoles(
            @Filter Specification<RoleEntity> spec, Pageable pageable) {
        BaseListResponse result = roleUseCase.getAllRoles(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
