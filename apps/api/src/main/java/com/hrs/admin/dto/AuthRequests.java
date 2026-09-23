package com.hrs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public final class AuthRequests {
    private AuthRequests() {
    }

    public static class Login {
        @NotBlank(message = "请输入登录账号")
        private String username;
        @NotBlank(message = "请输入登录密码")
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Profile {
        @NotBlank(message = "请输入昵称")
        private String nickname;
        private String avatar = "";
        private String phone = "";
        private String email = "";
        private String remark = "";
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class Password {
        @JsonProperty("old_password")
        @NotBlank(message = "请输入原密码")
        private String oldPassword;
        @JsonProperty("new_password")
        @NotBlank(message = "请输入新密码")
        private String newPassword;
        @JsonProperty("confirm_password")
        @NotBlank(message = "请确认新密码")
        private String confirmPassword;
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
