package com.hrs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class RoleRequest {
    @NotBlank(message = "请输入角色名称")
    private String name;
    @NotBlank(message = "请输入角色编码")
    private String code;
    private Integer status = 1;
    @JsonProperty("data_scope")
    private Integer dataScope = 2;
    private String remark = "";
    @JsonProperty("permission_ids")
    private List<Long> permissionIds = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
}
