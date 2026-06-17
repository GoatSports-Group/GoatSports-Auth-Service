package iuh.fit.auth.enums;

import lombok.Getter;

@Getter
public enum AuthProvider {
    LOCAL("Local"),
    KEYCLOAK("Keycloak"),
    ALL("Tất cả");

    private final String value;
    AuthProvider(String value) {
        this.value = value;
    }
}
