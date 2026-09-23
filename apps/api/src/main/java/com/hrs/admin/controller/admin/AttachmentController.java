package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.AttachmentService;
import com.hrs.admin.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin-api/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final QueryService queryService;

    public AttachmentController(AttachmentService attachmentService, QueryService queryService) {
        this.attachmentService = attachmentService;
        this.queryService = queryService;
    }

    @GetMapping("/config")
    public ApiResponse<?> config() {
        return ApiResponse.ok(attachmentService.config());
    }

    @GetMapping
    public ApiResponse<?> index(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                @RequestParam(name = "storage_type", required = false) String storageType) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(100, pageSize));
        return ApiResponse.ok(Map.of(
            "items", queryService.attachments(normalizedPage, normalizedPageSize, storageType),
            "total", queryService.attachmentCount(storageType),
            "page", normalizedPage,
            "page_size", normalizedPageSize
        ));
    }

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(attachmentService.upload(file, AdminContext.get().getId(), request), "上传成功");
    }

    @PostMapping("/credentials")
    public ApiResponse<?> credentials(@RequestBody Map<String, String> request) {
        return ApiResponse.ok(attachmentService.credentials(request.get("storage_type")));
    }

    @PostMapping("/record")
    public ApiResponse<?> record(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(attachmentService.record(request, AdminContext.get().getId(), httpRequest), "记录成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        attachmentService.delete(id, AdminContext.get().getId(), request);
        return ApiResponse.ok(null, "删除成功");
    }
}
