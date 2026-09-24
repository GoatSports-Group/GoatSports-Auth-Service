package iuh.fit.auth.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    @DisplayName("đủ 8 ký tự gồm chữ thường, chữ hoa, số và ký tự đặc biệt thì đạt")
    void strongPasswordPasses() {
        assertThat(PasswordPolicy.isStrong("Goat@2026")).isTrue();
    }

    @Test
    @DisplayName("thiếu bất kỳ loại ký tự nào, quá ngắn hoặc null đều không đạt")
    void weakPasswordsFail() {
        assertThat(PasswordPolicy.isStrong("goat@2026")).isFalse();   // thiếu chữ hoa
        assertThat(PasswordPolicy.isStrong("GOAT@2026")).isFalse();   // thiếu chữ thường
        assertThat(PasswordPolicy.isStrong("Goat@goat")).isFalse();   // thiếu số
        assertThat(PasswordPolicy.isStrong("Goat2026")).isFalse();    // thiếu ký tự đặc biệt
        assertThat(PasswordPolicy.isStrong("Go@26")).isFalse();       // quá ngắn
        assertThat(PasswordPolicy.isStrong(null)).isFalse();
    }
}
