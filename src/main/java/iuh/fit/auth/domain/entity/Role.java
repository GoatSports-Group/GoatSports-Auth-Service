package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.RoleEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {
    private UUID roleId;
    /** Vai trò hệ thống dùng tên trong {@link iuh.fit.auth.domain.enums.RoleEnum}; vai trò admin tự tạo có tên bất kỳ hợp lệ. */
    private String name;
    private String description;
    private boolean active;

    private Set<Permission> permissions;
}
