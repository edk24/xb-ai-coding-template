package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.ScheduledJobRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.ScheduledJobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/scheduled-jobs")
public class ScheduledJobController {
    private final ScheduledJobService scheduledJobService;

    public ScheduledJobController(ScheduledJobService scheduledJobService) {
        this.scheduledJobService = scheduledJobService;
    }

    @GetMapping
    public ApiResponse<?> jobs() {
        return ApiResponse.ok(scheduledJobService.jobs());
    }

    @GetMapping("/handlers")
    public ApiResponse<?> handlers() {
        return ApiResponse.ok(scheduledJobService.handlerOptions());
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(value = "job_id", required = false) Long jobId) {
        return ApiResponse.ok(scheduledJobService.logs(jobId));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody ScheduledJobRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(scheduledJobService.create(request, AdminContext.get().getId(), httpRequest), "任务已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody ScheduledJobRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(scheduledJobService.update(id, request, AdminContext.get().getId(), httpRequest), "任务已更新");
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                       HttpServletRequest httpRequest) {
        Object value = request.get("status");
        Integer status = value instanceof Number number ? number.intValue() : 0;
        return ApiResponse.ok(scheduledJobService.updateStatus(id, status, AdminContext.get().getId(), httpRequest), "任务状态已更新");
    }

    @PostMapping("/{id}/run")
    public ApiResponse<?> run(@PathVariable Long id) {
        return ApiResponse.ok(scheduledJobService.runManual(id, AdminContext.get().getId()), "任务已执行");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        scheduledJobService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "任务已删除");
    }
}
