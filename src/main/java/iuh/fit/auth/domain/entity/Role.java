package iuh.fit.auth.domain.entity;

import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private UUID roleId;
    private String name;
    private String description;
    private boolean active;

    private Set<Permission> permissions;
}
