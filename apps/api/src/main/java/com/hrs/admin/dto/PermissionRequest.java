package com.hrs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class PermissionRequest {
    @JsonProperty("parent_id")
    private Long parentId = 0L;
    @NotBlank(message = "请输入节点名称")
    private String name;
    @NotBlank(message = "请选择节点类型")
    private String type;
    @JsonProperty("route_path")
    private String routePath = "";
    @JsonProperty("component_path")
    private String componentPath = "";
    @JsonProperty("permission_key")
    @NotBlank(message = "请输入权限标识")
    private String permissionKey;
    private String icon = "";
    private Integer sort = 0;
    private Integer hidden = 0;
    private Integer status = 1;
    private String remark = "";

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    public String getComponentPath() { return componentPath; }
    public void setComponentPath(String componentPath) { this.componentPath = componentPath; }
    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getHidden() { return hidden; }
    public void setHidden(Integer hidden) { this.hidden = hidden; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
