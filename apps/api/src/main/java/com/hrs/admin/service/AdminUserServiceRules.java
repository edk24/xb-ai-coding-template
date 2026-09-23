package com.hrs.admin.service;

import com.hrs.admin.exception.BusinessException;

public final class AdminUserServiceRules {
    private AdminUserServiceRules() {
    }

    public static void ensureSuperAdminCanUseStatus(boolean superAdmin, int status) {
        if (superAdmin && status == 0) {
            throw new BusinessException("超级管理员不允许禁用");
        }
    }

    public static void ensureCanDelete(boolean superAdmin, boolean deletingSelf) {
        if (superAdmin) {
            throw new BusinessException("超级管理员不允许删除");
        }
        if (deletingSelf) {
            throw new BusinessException("不能删除当前登录账号");
        }
    }

    public static void ensureCanResetPassword(boolean superAdmin) {
        if (superAdmin) {
            throw new BusinessException("超级管理员不允许重置密码");
        }
    }
}
