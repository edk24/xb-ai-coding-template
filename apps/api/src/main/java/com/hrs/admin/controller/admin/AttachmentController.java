package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.security.RequirePermission;
import com.hrs.admin.service.AttachmentService;
import com.hrs.admin.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @RequirePermission("attachments:view")
    public ApiResponse<?> config() {
        return ApiResponse.ok(attachmentService.config());
    }

    @GetMapping
    @RequirePermission("attachments:view")
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

    /** 头像上传等个人场景对所有登录用户开放，只做大小与类型校验，不额外要求权限。 */
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(attachmentService.upload(file, AdminContext.get().getId(), request), "上传成功");
    }

    @PostMapping("/credentials")
    @RequirePermission("attachments:upload")
    public ApiResponse<?> credentials(@RequestBody Map<String, String> request) {
        return ApiResponse.ok(attachmentService.credentials(request.get("storage_type")));
    }

    @PostMapping("/record")
    @RequirePermission("attachments:upload")
    public ApiResponse<?> record(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(attachmentService.record(request, AdminContext.get().getId(), httpRequest), "记录成功");
    }

    /** 受鉴权的附件下载/预览：本地附件流式输出，COS/OSS 附件 302 跳到对象存储。 */
    @GetMapping("/{id}/file")
    public ResponseEntity<?> file(@PathVariable Long id) {
        // 不在注解上统一要求 attachments:view，头像等本人附件需要所有登录用户可读；
        // 服务内按"超管 / attachments:view / 文件归属本人"放行，避免按顺序 id 枚举他人附件。
        AttachmentService.Download download = attachmentService.download(id, AdminContext.get());
        if (download.redirectUrl() != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, download.redirectUrl())
                .build();
        }
        MediaType contentType = renderContentType(download.mimeType());
        boolean inline = !MediaType.APPLICATION_OCTET_STREAM.equals(contentType);
        ContentDisposition disposition = inline
            ? ContentDisposition.inline().filename(download.name(), StandardCharsets.UTF_8).build()
            : ContentDisposition.attachment().filename(download.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(new FileSystemResource(download.file()));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("attachments:delete")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        attachmentService.delete(id, AdminContext.get().getId(), request);
        return ApiResponse.ok(null, "删除成功");
    }

    /**
     * 输出层再兜一层：只有图片（排除可执行脚本的 svg）、PDF、纯文本允许 inline 预览并沿用对应 Content-Type；
     * 其余（含历史脏数据里被伪报的 text/html 等）一律按 application/octet-stream + attachment 下载。
     */
    private MediaType renderContentType(String value) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(value);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String type = mediaType.getType().toLowerCase();
        String subtype = mediaType.getSubtype().toLowerCase();
        if ("image".equals(type) && !"svg+xml".equals(subtype)) {
            return mediaType;
        }
        if ("application".equals(type) && "pdf".equals(subtype)) {
            return mediaType;
        }
        if ("text".equals(type) && "plain".equals(subtype)) {
            return mediaType;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
