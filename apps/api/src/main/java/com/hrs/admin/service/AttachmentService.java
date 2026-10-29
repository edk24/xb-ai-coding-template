package com.hrs.admin.service;

import com.hrs.admin.config.AdminApiProperties;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AdminUserProfile;
import com.hrs.admin.vo.AttachmentRow;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import com.tencent.cloud.CosStsClient;
import com.tencent.cloud.Response;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final JdbcTemplate jdbc;
    private final ConfigItemService configItemService;
    private final LogService logService;
    private final AdminApiProperties properties;

    public AttachmentService(JdbcTemplate jdbc, ConfigItemService configItemService, LogService logService,
                             AdminApiProperties properties) {
        this.jdbc = jdbc;
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
        // 存储驱动配置为 COS 时，附件必须由服务端写入对象存储，不能落本地磁盘。
        if ("cos".equals(configItemService.configValue("storage_driver", "local"))) {
            return uploadToCos(file, originalName, ext, userId, request);
        }
        return uploadToLocal(file, originalName, ext, userId, request);
    }

    private AttachmentRow uploadToLocal(MultipartFile file, String originalName, String ext, Long userId,
                                        HttpServletRequest request) {
        String dateDir = DATE_DIR.format(LocalDate.now());
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + (ext.isBlank() ? "" : "." + ext);
        Path relative = Path.of(dateDir, filename);
        Path root = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("附件写入本地磁盘失败: {}", target, e);
            throw new BusinessException("上传失败", 500);
        }
        String path = relative.toString().replace('\\', '/');
        // 本地附件不再对外公开静态目录，url 留空，由 file_url 指向受鉴权的下载接口。
        // mime_type 由服务端按扩展名映射，不信任客户端 multipart 里声明的 Content-Type。
        Long id = insert(userId, originalName, file.getSize(), safeMimeType(ext), "local", path, "");
        logService.operation(userId, "附件中心", "上传附件", request, Map.of("name", originalName, "size", file.getSize()), Map.of("id", id));
        return find(id);
    }

    private AttachmentRow uploadToCos(MultipartFile file, String originalName, String ext, Long userId,
                                      HttpServletRequest request) {
        String bucket = configItemService.configValue("cos_bucket", "");
        String region = configItemService.configValue("cos_region", "");
        String secretId = configItemService.configValue("cos_secret_id", "");
        String secretKey = configItemService.configValue("cos_secret_key", "");
        if (bucket.isBlank() || region.isBlank() || secretId.isBlank() || secretKey.isBlank()) {
            throw new BusinessException("COS 存储配置不完整，无法上传附件", 400);
        }
        String objectKey = normalizeUploadPrefix(configItemService.configValue("cos_allow_prefix", "uploads"))
            + "/" + DATE_DIR.format(LocalDate.now()) + "/" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().substring(0, 8) + (ext.isBlank() ? "" : "." + ext);
        COSClient cosClient = createCosClient(secretId, secretKey, region);
        String mimeType = safeMimeType(ext);
        try (var input = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(mimeType);
            cosClient.putObject(bucket, objectKey, input, metadata);
        } catch (Exception e) {
            log.error("上传文件到 COS 失败: {}", objectKey, e);
            throw new BusinessException("上传文件到 COS 失败", 502);
        } finally {
            cosClient.shutdown();
        }
        String url = cosUrl(bucket, region, objectKey);
        Long id = insert(userId, originalName, file.getSize(), mimeType, "cos", objectKey, url);
        logService.operation(userId, "附件中心", "上传附件", request,
            Map.of("name", originalName, "size", file.getSize(), "storage_type", "cos"), Map.of("id", id));
        return find(id);
    }

    private COSClient createCosClient(String secretId, String secretKey, String region) {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        return new COSClient(credentials, new ClientConfig(new Region(region)));
    }

    /** 配了 CDN 加速域名就用它拼接访问地址，否则回落到 COS 默认域名。 */
    private String cosUrl(String bucket, String region, String objectKey) {
        String cdnUrl = configItemService.configValue("cos_cdn_url", "").replaceAll("/+$", "");
        if (!cdnUrl.isBlank()) {
            return cdnUrl + "/" + objectKey.replaceAll("^/+", "");
        }
        return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + objectKey;
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
        if (name.length() > 255 || path.length() > 500) {
            throw new BusinessException("附件名称或路径过长", 422);
        }
        String mimeType = str(payload.get("mime_type"));
        if (mimeType.length() > 127) {
            throw new BusinessException("附件类型过长", 422);
        }
        // COS/OSS 附件下载是 302 到对象存储、Content-Type 由存储侧决定，
        // 所以记录层只接受安全白名单，其余收敛为 application/octet-stream，堵住 text/html 等同源 XSS。
        mimeType = recordMimeType(mimeType);
        Object sizeValue = payload.get("size");
        if (!(sizeValue instanceof Number number)) {
            throw new BusinessException("附件大小不合法", 422);
        }
        long size = number.longValue();
        if (size < 0 || size > properties.getAttachmentMaxSize()) {
            throw new BusinessException("附件大小不合法", 422);
        }
        String url = str(payload.get("url"));
        if (url.isBlank()) {
            String cdnUrl = str(payload.get("cdn_url")).replaceAll("/+$", "");
            url = cdnUrl.isBlank() ? "" : cdnUrl + "/" + path.replaceAll("^/+", "");
        }
        // 客户端只能记录对象存储的地址；地址必须为空或 http(s)，避免 javascript: 等伪协议进入列表形成 XSS。
        if (!url.isEmpty() && !allowedAttachmentUrl(url)) {
            throw new BusinessException("附件地址不合法", 422);
        }
        Long id = insert(userId, name, size, mimeType, storageType, path, url);
        logService.operation(userId, "附件中心", "上传附件", request, Map.of("name", name, "storage_type", storageType), Map.of("id", id));
        return find(id);
    }

    /** 附件下载描述：本地文件输出文件流，COS/OSS 附件 302 跳转到对象存储地址。 */
    public record Download(String name, String mimeType, Path file, String redirectUrl) {
    }

    public Download download(Long id, AdminUserProfile currentUser) {
        Map<String, Object> row = raw(id);
        authorizeDownload(row, currentUser);
        if ("local".equals(Rows.str(row, "storage_type"))) {
            Path file = resolveLocalFile(Rows.str(row, "path"));
            if (!Files.isRegularFile(file)) {
                throw new BusinessException("附件不存在", 404);
            }
            String mimeType = Rows.str(row, "mime_type");
            if (mimeType.isBlank()) {
                try {
                    mimeType = Files.probeContentType(file);
                } catch (IOException e) {
                    log.warn("探测附件类型失败: {}", file, e);
                }
            }
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }
            return new Download(Rows.str(row, "name"), mimeType, file, null);
        }
        String url = Rows.str(row, "url");
        // 只允许跳转到 http/https，且禁止换行等控制字符（防响应头注入），否则按不存在处理。
        boolean httpUrl = url.startsWith("http://") || url.startsWith("https://");
        if (!httpUrl || url.indexOf('\r') >= 0 || url.indexOf('\n') >= 0) {
            throw new BusinessException("附件不存在", 404);
        }
        return new Download(Rows.str(row, "name"), "", null, url);
    }

    /**
     * 下载鉴权：超管或拥有附件查看权限的账号可下载全部附件；
     * 其他账号只能下载本人上传的文件（如个人头像），避免按顺序 id 枚举他人附件。
     */
    private void authorizeDownload(Map<String, Object> row, AdminUserProfile currentUser) {
        if (currentUser == null) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
        if (Boolean.TRUE.equals(currentUser.getSuperAdmin())) {
            return;
        }
        List<String> keys = currentUser.getPermissionKeys();
        if (keys != null && keys.contains("attachments:view")) {
            return;
        }
        if (currentUser.getId() != null && currentUser.getId().equals(Rows.lng(row, "admin_user_id"))) {
            return;
        }
        throw new BusinessException("没有该操作权限", 403);
    }

    public Map<String, Object> credentials(String type) {
        // 前端直传目前只实现了 COS 的临时密钥签发。
        if (!"cos".equals(type)) {
            throw new BusinessException("当前仅支持 COS 临时凭证", 400);
        }
        String bucket = configItemService.configValue(type + "_bucket", "");
        String region = configItemService.configValue(type + "_region", "");
        String secretId = configItemService.configValue(type + "_secret_id", "");
        String secretKey = configItemService.configValue(type + "_secret_key", "");
        if (bucket.isBlank() || region.isBlank() || secretId.isBlank() || secretKey.isBlank()) {
            throw new BusinessException(type.toUpperCase() + " 存储配置不完整", 400);
        }
        String pathPrefix = normalizeUploadPrefix(configItemService.configValue(type + "_allow_prefix", "uploads"))
            + "/" + DATE_DIR.format(LocalDate.now());
        Response response = createCosCredentials(secretId, secretKey, bucket, region, pathPrefix);
        return Map.of(
            "storage_type", type,
            "credentials", Map.of(
                "tmpSecretId", response.credentials.tmpSecretId,
                "tmpSecretKey", response.credentials.tmpSecretKey,
                "sessionToken", response.credentials.sessionToken
            ),
            "region", region,
            "bucket", bucket,
            "path_prefix", pathPrefix,
            "cdn_url", configItemService.configValue(type + "_cdn_url", ""),
            "start_time", response.startTime,
            "expired_time", response.expiredTime
        );
    }

    /** 申请 COS 临时密钥，只授权当天目录的写入动作。 */
    private Response createCosCredentials(String secretId, String secretKey, String bucket, String region, String pathPrefix) {
        TreeMap<String, Object> config = new TreeMap<>();
        config.put("secretId", secretId);
        config.put("secretKey", secretKey);
        config.put("durationSeconds", credentialDurationSeconds());
        config.put("bucket", bucket);
        config.put("region", region);
        config.put("allowPrefixes", new String[] { pathPrefix + "/*" });
        config.put("allowActions", new String[] {
            "name/cos:PutObject",
            "name/cos:PostObject",
            "name/cos:InitiateMultipartUpload",
            "name/cos:ListMultipartUploads",
            "name/cos:ListParts",
            "name/cos:UploadPart",
            "name/cos:CompleteMultipartUpload"
        });
        try {
            return CosStsClient.getCredential(config);
        } catch (Exception e) {
            log.error("获取 COS 临时凭证失败", e);
            throw new BusinessException("获取 COS 临时凭证失败", 502);
        }
    }

    /** 临时密钥有效期，解析失败兜底 1800 秒，并限制在 300 ~ 7200 秒之间。 */
    private int credentialDurationSeconds() {
        String raw = configItemService.configValue("cos_duration", "1800");
        try {
            int seconds = Integer.parseInt(raw);
            return Math.max(300, Math.min(seconds, 7200));
        } catch (NumberFormatException e) {
            return 1800;
        }
    }

    private String normalizeUploadPrefix(String prefix) {
        String cleaned = prefix == null ? "" : prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        return cleaned.isBlank() ? "uploads" : cleaned;
    }

    @Transactional
    public void delete(Long id, Long userId, HttpServletRequest request) {
        Map<String, Object> row = raw(id);
        if ("local".equals(Rows.str(row, "storage_type"))) {
            try {
                Files.deleteIfExists(resolveLocalFile(Rows.str(row, "path")));
            } catch (IOException e) {
                // 数据记录删除优先，文件删除失败不阻塞管理操作。
                log.warn("删除本地附件文件失败: {}", Rows.str(row, "path"), e);
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
        return attachmentRow(raw(id));
    }

    private Map<String, Object> raw(Long id) {
        return jdbc.queryForList("SELECT * FROM attachments WHERE id = ?", id).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException("附件不存在", 404));
    }

    private AttachmentRow attachmentRow(Map<String, Object> row) {
        AttachmentRow item = new AttachmentRow();
        item.setId(Rows.lng(row, "id"));
        item.setAdminUserId(Rows.lng(row, "admin_user_id"));
        item.setName(Rows.str(row, "name"));
        item.setSize(Rows.lng(row, "size"));
        item.setMimeType(Rows.str(row, "mime_type"));
        String storageType = Rows.str(row, "storage_type");
        item.setStorageType(storageType);
        // 本地附件只暴露受鉴权的下载接口地址，服务端 path 不对外返回。
        item.setFileUrl("local".equals(storageType)
            ? "/admin-api/attachments/" + item.getId() + "/file"
            : Rows.str(row, "url"));
        item.setCreatedAt(Rows.date(row, "created_at"));
        return item;
    }

    /** 把记录里的相对路径解析到上传根目录下，并确保没有越出根目录（防路径穿越）。 */
    private Path resolveLocalFile(String path) {
        Path root = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        if (path.isBlank()) {
            throw new BusinessException("附件不存在", 404);
        }
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root)) {
            throw new BusinessException("附件不存在", 404);
        }
        return file;
    }

    private boolean allowedAttachmentUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/admin-api/attachments/");
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private boolean allowed(String ext) {
        if (ext.isBlank()) {
            return false;
        }
        return ("," + properties.getAttachmentAllowedExtensions().toLowerCase() + ",").contains("," + ext + ",");
    }

    /**
     * 按扩展名给出落库/输出用的 MIME 类型，避免直接信任客户端声明的 Content-Type
     * （例如把 .txt 声明成 text/html 造成同源 XSS）。
     */
    private String safeMimeType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }

    /** record 接口落库前的 MIME 白名单（大小写不敏感）：非白名单一律按二进制处理。 */
    private String recordMimeType(String mimeType) {
        String normalized = mimeType == null ? "" : mimeType.trim().toLowerCase();
        return switch (normalized) {
            case "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
                 "application/pdf", "text/plain",
                 "video/mp4", "audio/mpeg", "audio/mp4", "audio/aac", "audio/wav" -> normalized;
            default -> "application/octet-stream";
        };
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
