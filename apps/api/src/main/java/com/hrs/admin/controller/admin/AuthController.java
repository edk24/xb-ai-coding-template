package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.AuthRequests;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        return ApiResponse.ok(authService.login(request, httpRequest, httpResponse), "登录成功");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(AdminContext.get().getId(), httpRequest, httpResponse);
        return ApiResponse.ok(null, "退出成功");
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        return ApiResponse.ok(authService.profileById(AdminContext.get().getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@Valid @RequestBody AuthRequests.Profile request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.updateProfile(AdminContext.get().getId(), request, httpRequest), "资料已更新");
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody AuthRequests.Password request, HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        authService.updatePassword(AdminContext.get().getId(), request, httpRequest, httpResponse);
        return ApiResponse.ok(null, "密码修改成功，请重新登录");
    }
}
