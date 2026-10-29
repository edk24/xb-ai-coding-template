package com.hrs.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    @Test
    void createsTokenWithAdminPayloadAndParsesIt() {
        JwtTokenService service = new JwtTokenService("test-secret-for-admin-api-0123456789abcdef", 7200);

        String token = service.createToken(7L, "operator", false, 3);
        CurrentAdminUser parsed = service.parseToken(token);

        assertThat(parsed.id()).isEqualTo(7L);
        assertThat(parsed.username()).isEqualTo("operator");
        assertThat(parsed.superAdmin()).isFalse();
        assertThat(parsed.tokenVersion()).isEqualTo(3);
    }
}
