package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.permission.CreatePermissionRequestCommand;
import iuh.fit.auth.application.dto.request.permission.UpdatePermissionRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;
import iuh.fit.auth.application.mapper.PermissionMapper;
import iuh.fit.auth.application.service.PermissionService;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.PermissionConverter;
import iuh.fit.auth.presentation.request.permission.PermissionCreateRequest;
import iuh.fit.auth.presentation.request.permission.PermissionUpdateRequest;
import iuh.fit.auth.application.dto.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.permission.PermissionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class PermissionAdminController {
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionCreateRequest request)
            throws ServiceException {
        if (permissionService.existsPermission(request.getApiPath(), request.getModule(), request.getMethod())) {
            throw new ServiceException("Quyền hạn đã tồn tại");
        }
        CreatePermissionRequestCommand command = new CreatePermissionRequestCommand(
                request.getName(),
                request.getApiPath(),
                request.getMethod(),
                request.getModule()
        );
        PermissionResponseCommand resDto = permissionMapper.toPermissionResponseCommand(permissionService.createPermission(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionConverter.toResponse(resDto));
    }

    @PutMapping
    public ResponseEntity<PermissionResponse> updatePermission(@Valid @RequestBody PermissionUpdateRequest request)
            throws ServiceException {
        if (permissionService.getPermissionById(request.getPermissionId()) == null) {
            throw new ServiceException("Quyền hạn không tồn tại");
        }
        UpdatePermissionRequestCommand command = new UpdatePermissionRequestCommand(
                request.getPermissionId(),
                request.getName(),
                request.getApiPath(),
                request.getMethod(),
                request.getModule()
        );
        PermissionResponseCommand resDto = permissionMapper.toPermissionResponseCommand(permissionService.updatePermission(command));
        return ResponseEntity.status(HttpStatus.OK).body(permissionConverter.toResponse(resDto));
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
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable("id") String id) throws ServiceException {
        PermissionResponseCommand resDto = permissionMapper.toPermissionResponseCommand(permissionService.getPermissionById(id));
        return ResponseEntity.status(HttpStatus.OK).body(permissionConverter.toResponse(resDto));
    }

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllPermissions(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<PermissionResponseCommand> resultDto = permissionMapper.toPermissionResponseCommand(permissionService.getAllPermissions(query));

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<PermissionResponse> permissionResponses = resultDto.items().stream()
                .map(permissionConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, permissionResponses));
    }
}
