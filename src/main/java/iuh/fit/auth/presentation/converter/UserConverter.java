package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.presentation.response.report.UserDetailReportResponse;
import iuh.fit.auth.presentation.response.report.UserReportResponse;
import iuh.fit.auth.presentation.response.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "hasPassword", expression = "java(user.password() != null && !user.password().isBlank())")
    UserResponse toResponse(UserResponseCommand user);

    UserReportResponse toResponse(UserReportResponseCommand user);

    @Mapping(target = "status", expression = "java(user.status() != null ? user.status().getValue() : null)")
    @Mapping(target = "roleName", expression = "java(user.role() != null ? user.role().getName() : \"Unknown\")")
    UserDetailReportResponse toResponseUserDetailReport(UserResponseCommand user);
}
