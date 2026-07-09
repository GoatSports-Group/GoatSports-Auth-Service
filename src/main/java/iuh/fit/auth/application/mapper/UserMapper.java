package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.domain.entity.User;

public class UserMapper {

    public static UserResponseCommand toUserResponseCommand(User user){
        return new UserResponseCommand(
                user.getUserId(), user.getKeycloakId(), user.getEmail(), user.getUsername(),
                user.getPassword(), user.getFullName(), user.getAvatarUrl(), user.getStatus(),
                user.getGender(),user.getRole(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    public static User toUser(UserResponseCommand user){
        if(user == null) return null;

        return User.builder()
                .userId(user.userId())
                .keycloakId(user.keycloakId())
                .email(user.email())
                .username(user.username())
                .password(user.password())
                .fullName(user.fullName())
                .avatarUrl(user.avatarUrl())
                .status(user.status())
                .gender(user.gender())
                .role(user.role())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .build();
    }

    public static PageResult<UserResponseCommand> toUserResponseCommand(PageResult<User> users){
        return new PageResult<>(
                users.items().stream().map(UserMapper::toUserResponseCommand).toList(),
                users.total(), users.page(), users.pageSize(), users.totalPages()
        );
    }

}
