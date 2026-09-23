package com.hrs.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrs.admin.vo.DepartmentNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class TreeBuilderTest {
    @Test
    void buildsDepartmentTreeByParentIdAndSortOrder() {
        List<DepartmentNode> flat = List.of(
            node(3, 1, "研发二部", 2),
            node(1, 0, "总部", 0),
            node(2, 1, "研发一部", 1)
        );

        List<DepartmentNode> tree = TreeBuilder.departmentTree(flat);

        assertThat(tree).extracting(DepartmentNode::getId).containsExactly(1L);
        assertThat(tree.getFirst().getChildren())
            .extracting(DepartmentNode::getName)
            .containsExactly("研发一部", "研发二部");
    }

    private static DepartmentNode node(long id, long parentId, String name, int sort) {
        DepartmentNode node = new DepartmentNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setName(name);
        node.setSort(sort);
        return node;
    }
}
