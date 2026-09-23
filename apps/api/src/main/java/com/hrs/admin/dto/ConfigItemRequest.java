package com.hrs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ConfigItemRequest {
    @NotBlank(message = "请输入配置名称")
    private String name;
    @JsonProperty("group_name")
    private String groupName = "";
    @NotBlank(message = "请输入配置键名")
    private String key;
    private Object value = "";
    @NotBlank(message = "请选择配置类型")
    private String type = "input";
    private String options = "";
    private Integer sort = 0;
    private Integer status = 1;
    private String remark = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
