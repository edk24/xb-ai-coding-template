package com.hrs.admin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdminMapperIntegrationTest {
    @Autowired
    private AdminMapper adminMapper;

    @Test
    void mapperLoadsXmlSqlAndReadsSeedData() {
        List<Map<String, Object>> roles = adminMapper.selectRoles();
        List<Map<String, Object>> permissions = adminMapper.selectPermissions(false, null);

        assertThat(roles).extracting(row -> String.valueOf(row.get("code"))).contains("super_admin");
        assertThat(permissions).hasSizeGreaterThanOrEqualTo(23);
        assertThat(adminMapper.countAdminUsers()).isEqualTo(1);
    }
}
