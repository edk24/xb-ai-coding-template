package com.hrs.admin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class DepartmentNode {
    private Long id;
    private Long parentId;
    private String name = "";
    private String leader = "";
    private String phone = "";
    private Integer sort = 0;
    private Integer status = 1;
    private String remark = "";
    private List<DepartmentNode> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @JsonProperty("parent_id")
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<DepartmentNode> getChildren() { return children; }
    public void setChildren(List<DepartmentNode> children) { this.children = children; }
}
