package com.hrs.admin.service;

import com.hrs.admin.config.AdminApiProperties;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AttachmentRow;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final ConfigItemService configItemService;
    private final LogService logService;
    private final AdminApiProperties properties;

    public AttachmentService(JdbcTemplate jdbc, QueryService queryService, ConfigItemService configItemService,
                             LogService logService, AdminApiProperties properties) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.configItemService = configItemService;
        this.logService = logService;
        this.properties = properties;
    }

    public Map<String, Object> config() {
        return Map.of(
            "storage_type", configItemService.configValue("storage_driver", "local"),
            "max_file_size", properties.getAttachmentMaxSize(),
            "allowed_extensions", properties.getAttachmentAllowedExtensions()
        );
    }

    @Transactional
    public AttachmentRow upload(MultipartFile file, Long userId, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件", 400);
        }
        if (file.getSize() > properties.getAttachmentMaxSize()) {
            throw new BusinessException("文件大小超出限制", 400);
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(originalName);
        if (!allowed(ext)) {
            throw new BusinessException("文件类型不允许", 400);
        }
        String dateDir = DATE_DIR.format(LocalDate.now());
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + (ext.isBlank() ? "" : "." + ext);
        Path relative = Path.of(dateDir, filename);
        Path root = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("上传失败", 500);
        }
        String path = relative.toString().replace('\\', '/');
        String url = properties.getUploadUrlPrefix() + "/" + path;
        Long id = insert(userId, originalName, file.getSize(), file.getContentType(), "local", path, url);
        logService.operation(userId, "附件中心", "上传附件", request, Map.of("name", originalName, "size", file.getSize()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public AttachmentRow record(Map<String, Object> payload, Long userId, HttpServletRequest request) {
        String storageType = str(payload.get("storage_type"));
        if (!"cos".equals(storageType) && !"oss".equals(storageType)) {
            throw new BusinessException("不支持的存储类型，仅支持 cos/oss", 400);
        }
        String name = str(payload.get("name"));
        String path = str(payload.get("path"));
        if (name.isBlank() || path.isBlank()) {
            throw new BusinessException("附件名称和路径不能为空", 422);
        }
        long size = payload.get("size") instanceof Number number ? number.longValue() : 0L;
        String url = str(payload.get("url"));
        if (url.isBlank()) {
            String cdnUrl = str(payload.get("cdn_url")).replaceAll("/+$", "");
            url = cdnUrl.isBlank() ? "" : cdnUrl + "/" + path.replaceAll("^/+", "");
        }
        Long id = insert(userId, name, size, str(payload.get("mime_type")), storageType, path, url);
        logService.operation(userId, "附件中心", "上传附件", request, Map.of("name", name, "storage_type", storageType), Map.of("id", id));
        return find(id);
    }

    public Map<String, Object> credentials(String type) {
        if (!"cos".equals(type) && !"oss".equals(type)) {
            throw new BusinessException("不支持的存储类型，仅支持 cos/oss", 400);
        }
        String bucket = configItemService.configValue(type + "_bucket", "");
        String region = configItemService.configValue(type + "_region", "");
        if (bucket.isBlank() || region.isBlank()) {
            throw new BusinessException(type.toUpperCase() + " 存储配置不完整", 400);
        }
        return Map.of(
            "storage_type", type,
            "credentials", Map.of("tmpSecretId", "", "tmpSecretKey", "", "sessionToken", ""),
            "region", region,
            "bucket", bucket,
            "path_prefix", configItemService.configValue(type + "_allow_prefix", "uploads") + "/" + DATE_DIR.format(LocalDate.now()),
            "cdn_url", configItemService.configValue(type + "_cdn_url", ""),
            "expired_time", System.currentTimeMillis() / 1000 + Long.parseLong(configItemService.configValue(type + "_duration", "1800"))
        );
    }

    @Transactional
    public void delete(Long id, Long userId, HttpServletRequest request) {
        AttachmentRow row = find(id);
        if ("local".equals(row.getStorageType())) {
            try {
                Files.deleteIfExists(Path.of(properties.getUploadRoot()).resolve(row.getPath()).normalize());
            } catch (IOException ignored) {
                // 数据记录删除优先，文件删除失败不阻塞管理操作。
            }
        }
        jdbc.update("DELETE FROM attachments WHERE id = ?", id);
        logService.operation(userId, "附件中心", "删除附件", request, Map.of("id", id), Map.of("success", true));
    }

    private Long insert(Long userId, String name, long size, String mimeType, String storageType, String path, String url) {
        jdbc.update("""
            INSERT INTO attachments (admin_user_id, name, size, mime_type, storage_type, path, url, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            """, userId, name, size, mimeType == null ? "" : mimeType, storageType, path, url);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private AttachmentRow find(Long id) {
        return queryService.attachments(1, 100, "").stream()
            .filter(item -> item.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("附件不存在", 404));
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private boolean allowed(String ext) {
        if (ext.isBlank()) {
            return true;
        }
        return ("," + properties.getAttachmentAllowedExtensions().toLowerCase() + ",").contains("," + ext + ",");
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
