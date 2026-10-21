package iuh.fit.auth.domain.enums;

public enum UserStatusEnum {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    PENDING("Chờ xác thực"),
    BLOCKED("Đã khóa"),
    PENDING_DELETION("Chờ xóa"),
    DELETED("Đã xóa");

    private final String value;

    UserStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
