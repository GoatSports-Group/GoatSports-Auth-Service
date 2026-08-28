package iuh.fit.auth.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {
    private UUID permissionId;
    private String name;
    private String apiPath;
    private String method;
    private String module;
}
