package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.application.mapper.RoleMapper;
import iuh.fit.auth.application.service.RoleService;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.RoleConverter;
import iuh.fit.auth.presentation.request.role.RoleCreateRequest;
import iuh.fit.auth.presentation.request.role.RoleUpdateRequest;
import iuh.fit.auth.application.dto.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.role.RoleResponse;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ROLE')")
    @AuditLog(action = "CREATE_ROLE", description = "Tạo vai trò")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) throws ServiceException {
        CreateRoleRequestCommand command = new CreateRoleRequestCommand(request.getName(), request.getDescription());
        RoleResponseCommand resDto = roleMapper.toRoleResponseCommand(roleService.createRole(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(roleConverter.toResponse(resDto));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    @AuditLog(action = "UPDATE_ROLE", description = "Cập nhật vai trò #{#request.roleId}")
    public ResponseEntity<RoleResponse> updateRole(@Valid @RequestBody RoleUpdateRequest request) throws ServiceException {
        UpdateRoleRequestCommand command = new UpdateRoleRequestCommand(
                request.getRoleId(),
                request.getName(),
                request.getDescription(),
                request.isActive(),
                request.getPermissionIds()
        );
        RoleResponseCommand role = roleMapper.toRoleResponseCommand(roleService.updateRole(command));
        return ResponseEntity.status(HttpStatus.OK).body(roleConverter.toResponse(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    @AuditLog(action = "DELETE_ROLE", description = "Xóa vai trò #{#id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) throws ServiceException {
        roleService.deleteRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ACTIVATE_ROLE')")
    @AuditLog(action = "ACTIVATE_ROLE", description = "Bật vai trò #{#id}")
    public ResponseEntity<RoleResponse> activateRole(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = roleMapper.toRoleResponseCommand(roleService.activateRole(id));
        return ResponseEntity.status(HttpStatus.OK).body(roleConverter.toResponse(role));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVE_ROLE')")
    @AuditLog(action = "DEACTIVE_ROLE", description = "Tắt vai trò #{#id}")
    public ResponseEntity<RoleResponse> deactivateRole(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = roleMapper.toRoleResponseCommand(roleService.deactivateRole(id));
        return ResponseEntity.status(HttpStatus.OK).body(roleConverter.toResponse(role));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GET_ROLE')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = roleMapper.toRoleResponseCommand(roleService.getRoleById(id));
        return ResponseEntity.status(HttpStatus.OK).body(roleConverter.toResponse(role));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GET_ROLES')")
    public ResponseEntity<BaseListResponse> getAllRoles(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<RoleResponseCommand> resultDto = roleMapper.toRoleResponseCommand(roleService.getAllRoles(query));

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<RoleResponse> roleResponses = resultDto.items().stream()
                .map(roleConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, roleResponses));
    }
}
