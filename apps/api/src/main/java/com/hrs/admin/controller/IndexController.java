package com.hrs.admin.controller;

import com.hrs.admin.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {
    @GetMapping("/")
    public ApiResponse<Map<String, String>> index() {
        return ApiResponse.ok(Map.of("name", "Spring Boot Admin API", "status", "ok"));
    }
}
