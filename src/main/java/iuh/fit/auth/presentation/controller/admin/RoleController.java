package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.application.mapper.RoleMapper;
import iuh.fit.auth.application.usecase.RoleUseCase;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.RoleConverter;
import iuh.fit.auth.presentation.request.role.RoleCreateRequest;
import iuh.fit.auth.presentation.request.role.RoleUpdateRequest;
import iuh.fit.auth.application.dto.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.role.RoleResponse;
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
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleUseCase roleUseCase;

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        CreateRoleRequestCommand command = new CreateRoleRequestCommand(request.getName(), request.getDescription());
        RoleResponseCommand resDto = RoleMapper.toRoleResponseCommand(roleUseCase.createRole(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleConverter.toResponse(resDto));
    }

    @PutMapping
    public ResponseEntity<RoleResponse> updateRole(@Valid @RequestBody RoleUpdateRequest request) throws ServiceException {
        UpdateRoleRequestCommand command = new UpdateRoleRequestCommand(
                request.getRoleId(),
                request.getName(),
                request.getDescription(),
                request.isActive(),
                request.getPermissionIds()
        );
        RoleResponseCommand role = RoleMapper.toRoleResponseCommand(roleUseCase.updateRole(command));
        return ResponseEntity.status(HttpStatus.OK).body(RoleConverter.toResponse(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) throws ServiceException {
        roleUseCase.deleteRole(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<RoleResponse> activateRole(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = RoleMapper.toRoleResponseCommand(roleUseCase.activateRole(id));
        return ResponseEntity.status(HttpStatus.OK).body(RoleConverter.toResponse(role));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<RoleResponse> deactivateRole(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = RoleMapper.toRoleResponseCommand(roleUseCase.deactivateRole(id));
        return ResponseEntity.status(HttpStatus.OK).body(RoleConverter.toResponse(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable("id") String id) throws ServiceException {
        RoleResponseCommand role = RoleMapper.toRoleResponseCommand(roleUseCase.getRoleById(id));
        return ResponseEntity.status(HttpStatus.OK).body(RoleConverter.toResponse(role));
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllRoles(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<RoleResponseCommand> resultDto = RoleMapper.toRoleResponseCommand(roleUseCase.getAllRoles(query));

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<RoleResponse> roleResponses = resultDto.items().stream()
                .map(RoleConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, roleResponses));
    }
}
