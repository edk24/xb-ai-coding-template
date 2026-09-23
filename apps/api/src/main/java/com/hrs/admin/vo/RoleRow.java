package com.hrs.admin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class RoleRow {
    private Long id;
    private String name = "";
    private String code = "";
    private Integer status = 1;
    private Integer dataScope = 2;
    private String remark = "";
    private String createdAt = "";
    private String updatedAt = "";
    private List<Long> permissionIds = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    @JsonProperty("data_scope")
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    @JsonProperty("created_at")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    @JsonProperty("updated_at")
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    @JsonProperty("permission_ids")
    public List<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
}
