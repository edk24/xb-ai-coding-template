package com.hrs.admin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AttachmentRow {
    private Long id;
    private Long adminUserId;
    private String name = "";
    private Long size = 0L;
    private String mimeType = "";
    private String storageType = "";
    private String path = "";
    private String url = "";
    private String createdAt = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @JsonProperty("admin_user_id")
    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    @JsonProperty("mime_type")
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    @JsonProperty("storage_type")
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    @JsonProperty("created_at")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
