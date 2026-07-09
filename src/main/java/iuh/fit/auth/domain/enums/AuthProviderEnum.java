package iuh.fit.auth.domain.enums;

import lombok.Getter;

@Getter
public enum AuthProviderEnum {
    LOCAL("Local"),
    GOOGLE("Google"),
    ALL("Tất cả");

    private final String value;
    AuthProviderEnum(String value) {
        this.value = value;
    }
}
