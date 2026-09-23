package com.hrs.admin.security;

import com.hrs.admin.vo.AdminUserProfile;

public final class AdminContext {
    private static final ThreadLocal<AdminUserProfile> CURRENT = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(AdminUserProfile user) {
        CURRENT.set(user);
    }

    public static AdminUserProfile get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
