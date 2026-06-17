package iuh.fit.auth.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động");

    private final String value;

    UserStatusEnum(String value) {
        this.value = value;
    }
}
