package iuh.fit.auth.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "roles" })
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID permissionId;
    @NotBlank(message = "Tên không được để trống")
    private String name;
    @NotBlank(message = "Api không được để trống")
    private String apiPath;
    @NotBlank(message = "Phương thức không được để trống")
    private String method;
    @NotBlank(message = "Module không được để trống")
    private String module;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
