package iuh.fit.auth.domain.entity;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private UUID permissionId;
    private String name;
    private String apiPath;
    private String method;
    private String module;
}
