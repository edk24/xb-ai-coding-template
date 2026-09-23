package com.hrs.admin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigItemRow {
    private Long id;
    private String groupName = "";
    private String name = "";
    private String key = "";
    private String value = "";
    private String type = "";
    private String options = "";
    private Integer sort = 0;
    private Integer status = 1;
    private String remark = "";
    private String createdAt = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @JsonProperty("group_name")
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
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
    @JsonProperty("created_at")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
