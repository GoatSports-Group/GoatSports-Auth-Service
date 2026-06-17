package iuh.fit.auth.controller.admin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.request.role.RoleCreateRequest;
import iuh.fit.auth.dto.request.role.RoleUpdateRequest;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.service.RoleService;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleCreateRequest role) {
        Role res = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Role> updateRole(@Valid @RequestBody RoleUpdateRequest role) throws ServiceException {
        Role res = roleService.updateRole(role);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) throws ServiceException {
        roleService.deleteRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Role> activateRole(@PathVariable("id") String id) throws ServiceException {
        Role res = roleService.activateRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Role> deactivateRole(@PathVariable("id") String id) throws ServiceException {
        Role res = roleService.deactivateRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable("id") String id) throws ServiceException {
        Role role = roleService.getRoleById(id);
        return ResponseEntity.status(HttpStatus.OK).body(role);
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllRoles(
            @Filter Specification<Role> spec, Pageable pageable) {
        BaseListResponse result = roleService.getAllRoles(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
