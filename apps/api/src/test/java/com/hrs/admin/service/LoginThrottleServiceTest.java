package com.hrs.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginThrottleServiceTest {
    private final LoginThrottleService service = new LoginThrottleService();

    @Test
    void locksSameUsernameAndIpAfterFiveFailures() {
        for (int i = 0; i < LoginThrottleService.MAX_FAILURES - 1; i++) {
            service.recordFailure("Admin", "127.0.0.1");
        }
        assertThat(service.isLocked("admin", "127.0.0.1")).isFalse();

        service.recordFailure("Admin", "127.0.0.1");

        assertThat(service.isLocked("admin", "127.0.0.1")).isTrue();
        assertThat(service.remainingMinutes("admin", "127.0.0.1")).isBetween(1L, 15L);
    }

    @Test
    void doesNotLockOtherIpsOrOtherUsernames() {
        for (int i = 0; i < LoginThrottleService.MAX_FAILURES; i++) {
            service.recordFailure("admin", "10.0.0.1");
        }

        assertThat(service.isLocked("admin", "10.0.0.1")).isTrue();
        assertThat(service.isLocked("admin", "10.0.0.2")).isFalse();
        assertThat(service.isLocked("operator", "10.0.0.1")).isFalse();
    }

    @Test
    void successfulLoginClearsFailureCount() {
        for (int i = 0; i < LoginThrottleService.MAX_FAILURES - 1; i++) {
            service.recordFailure("admin", "127.0.0.1");
        }
        service.clear("admin", "127.0.0.1");

        for (int i = 0; i < LoginThrottleService.MAX_FAILURES - 1; i++) {
            service.recordFailure("admin", "127.0.0.1");
        }

        assertThat(service.isLocked("admin", "127.0.0.1")).isFalse();
        assertThat(service.remainingMinutes("admin", "127.0.0.1")).isZero();
    }
}
