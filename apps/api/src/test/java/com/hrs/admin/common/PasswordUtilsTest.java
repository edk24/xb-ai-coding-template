package com.hrs.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordUtilsTest {
    @Test
    void generatesThirtyTwoCharacterHexSalt() {
        assertThat(PasswordUtils.newSalt()).matches("[0-9a-f]{32}");
    }

    @Test
    void hashesPasswordWithPbkdf2AndVerifiesIt() {
        String salt = PasswordUtils.newSalt();
        String hash = PasswordUtils.hash("abc12345", salt);

        assertThat(hash).matches("[0-9a-f]{32}");
        assertThat(PasswordUtils.verify("abc12345", hash, salt)).isTrue();
        assertThat(PasswordUtils.verify("abc123456", hash, salt)).isFalse();
    }

    @Test
    void verifiesLegacyMd5HashForExistingData() {
        String salt = "9f2f5c0d";
        String legacyHash = PasswordUtils.md5Hex(PasswordUtils.md5Hex("123456") + salt);

        assertThat(legacyHash).isEqualTo("be952ae5498a25413eae74b5bbd4b7fa");
        assertThat(PasswordUtils.verify("123456", legacyHash, salt)).isTrue();
        assertThat(PasswordUtils.verify("654321", legacyHash, salt)).isFalse();
        assertThat(PasswordUtils.isLegacySalt(salt)).isTrue();
    }

    @Test
    void returnsFalseForMalformedHashOrSaltInsteadOfThrowing() {
        assertThat(PasswordUtils.verify("123456", null, null)).isFalse();
        assertThat(PasswordUtils.verify("123456", "abc", null)).isFalse();
        assertThat(PasswordUtils.verify("123456", "abc", "")).isFalse();
        assertThat(PasswordUtils.verify("123456", "abc", "zzzz")).isFalse();
        assertThat(PasswordUtils.verify("123456", "abc", "0123456789abcdef0123456789abcdef0")).isFalse();
        assertThat(PasswordUtils.verify("123456", "abc", "0123456789abcdef0123456789abcdef!")).isFalse();
    }
}
