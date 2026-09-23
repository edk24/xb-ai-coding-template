package com.hrs.admin.service;

import com.hrs.admin.vo.DepartmentNode;
import com.hrs.admin.vo.PermissionNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TreeBuilder {
    private TreeBuilder() {
    }

    public static List<DepartmentNode> departmentTree(List<DepartmentNode> flat) {
        Map<Long, DepartmentNode> byId = new LinkedHashMap<>();
        flat.stream()
            .sorted(Comparator.comparing(DepartmentNode::getSort).thenComparing(DepartmentNode::getId))
            .forEach(node -> {
                node.setChildren(new ArrayList<>());
                byId.put(node.getId(), node);
            });

        List<DepartmentNode> roots = new ArrayList<>();
        for (DepartmentNode node : byId.values()) {
            DepartmentNode parent = byId.get(node.getParentId());
            if (parent == null || node.getParentId() == 0) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    public static List<PermissionNode> permissionTree(List<PermissionNode> flat) {
        Map<Long, PermissionNode> byId = new LinkedHashMap<>();
        flat.stream()
            .sorted(Comparator.comparing(PermissionNode::getSort).thenComparing(PermissionNode::getId))
            .forEach(node -> {
                node.setChildren(new ArrayList<>());
                byId.put(node.getId(), node);
            });

        List<PermissionNode> roots = new ArrayList<>();
        for (PermissionNode node : byId.values()) {
            PermissionNode parent = byId.get(node.getParentId());
            if (parent == null || node.getParentId() == 0) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
