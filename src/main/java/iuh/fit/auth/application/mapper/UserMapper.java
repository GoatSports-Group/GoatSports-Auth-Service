package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.domain.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseCommand toUserResponseCommand(User user);

    List<UserResponseCommand> toUserResponseCommandList(List<User> users);

    default PageResult<UserResponseCommand> toUserResponseCommand(PageResult<User> users) {
        if (users == null) return null;
        return new PageResult<>(
                toUserResponseCommandList(users.items()),
                users.total(),
                users.page(),
                users.pageSize(),
                users.totalPages()
        );
    }
}
