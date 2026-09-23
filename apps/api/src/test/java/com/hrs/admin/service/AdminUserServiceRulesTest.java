package com.hrs.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrs.admin.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AdminUserServiceRulesTest {
    @Test
    void rejectsDisablingSuperAdmin() {
        assertThatThrownBy(() -> AdminUserServiceRules.ensureSuperAdminCanUseStatus(true, 0))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("超级管理员");
    }

    @Test
    void rejectsResettingSuperAdminPassword() {
        assertThatThrownBy(() -> AdminUserServiceRules.ensureCanResetPassword(true))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("超级管理员");
    }
}
