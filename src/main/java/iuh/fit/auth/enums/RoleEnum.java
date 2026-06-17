package iuh.fit.auth.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN("Người quản trị"),
    PLAYER("Tuyển thủ"),
    VENUE_OWNER("Chủ sân");

    private final String value;
    RoleEnum(String value) {
        this.value = value;
    }
}
