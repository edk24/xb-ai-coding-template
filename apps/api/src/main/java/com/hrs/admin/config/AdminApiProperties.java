package com.hrs.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin-api")
public class AdminApiProperties {
    private String jwtSecret = "change-me-admin-system-secret";
    private long jwtExpireSeconds = 7200;
    private String uploadRoot = "storage/attachments";
    private String uploadUrlPrefix = "/storage/attachments";
    private long attachmentMaxSize = 52428800;
    private String attachmentAllowedExtensions = "jpg,jpeg,png,gif,bmp,webp,doc,docx,xls,xlsx,pdf,txt,zip,rar,mp4,avi,mp3,m4a,aac,wav";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpireSeconds() {
        return jwtExpireSeconds;
    }

    public void setJwtExpireSeconds(long jwtExpireSeconds) {
        this.jwtExpireSeconds = jwtExpireSeconds;
    }

    public String getUploadRoot() {
        return uploadRoot;
    }

    public void setUploadRoot(String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    public String getUploadUrlPrefix() {
        return uploadUrlPrefix;
    }

    public void setUploadUrlPrefix(String uploadUrlPrefix) {
        this.uploadUrlPrefix = uploadUrlPrefix;
    }

    public long getAttachmentMaxSize() {
        return attachmentMaxSize;
    }

    public void setAttachmentMaxSize(long attachmentMaxSize) {
        this.attachmentMaxSize = attachmentMaxSize;
    }

    public String getAttachmentAllowedExtensions() {
        return attachmentAllowedExtensions;
    }

    public void setAttachmentAllowedExtensions(String attachmentAllowedExtensions) {
        this.attachmentAllowedExtensions = attachmentAllowedExtensions;
    }
}
