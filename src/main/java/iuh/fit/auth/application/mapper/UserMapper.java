package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.domain.entity.User;

public class UserMapper {

    public static UserResponseCommand toUserResponseCommand(User user){
        return new UserResponseCommand(
                user.getUserId(), user.getKeycloakId(), user.getEmail(), user.getUsername(),
                user.getPassword(), user.getFullName(), user.getAvatarUrl(), user.getStatus(),
                user.getAuthProvider(), user.getRole(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    public static User toUser(UserResponseCommand user){
        return new User(
                user.userId(), user.keycloakId(), user.email(), user.username(),
                user.password(), user.fullName(), user.avatarUrl(), user.status(),
                user.authProvider(), user.role(), user.createdAt(), user.updatedAt()
        );
    }

    public static PageResult<UserResponseCommand> toUserResponseCommand(PageResult<User> users){
        return new PageResult<>(
                users.items().stream().map(UserMapper::toUserResponseCommand).toList(),
                users.total(), users.page(), users.pageSize(), users.totalPages()
        );
    }

}
