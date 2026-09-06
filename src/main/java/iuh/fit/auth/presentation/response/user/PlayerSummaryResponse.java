package iuh.fit.auth.presentation.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSummaryResponse {
    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
}
