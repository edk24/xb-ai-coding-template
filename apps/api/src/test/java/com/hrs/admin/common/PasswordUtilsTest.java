package com.hrs.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordUtilsTest {
    @Test
    void hashesClientMd5WithSaltForExistingFrontendCompatibility() {
        String clientMd5 = PasswordUtils.md5Hex("123456");

        assertThat(PasswordUtils.adminPasswordHash(clientMd5, "9f2f5c0d"))
            .isEqualTo("be952ae5498a25413eae74b5bbd4b7fa");
    }

    @Test
    void generatesEightCharacterHexSalt() {
        String salt = PasswordUtils.randomSalt();

        assertThat(salt).matches("[0-9a-f]{8}");
    }
}
