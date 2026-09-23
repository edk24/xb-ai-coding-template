package com.hrs.admin.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {
    List<Map<String, Object>> selectAdminUsers();

    List<Map<String, Object>> selectAdminUserRoles();

    List<Map<String, Object>> selectRoles();

    List<Map<String, Object>> selectRolePermissions();

    List<Map<String, Object>> selectDepartments();

    List<Map<String, Object>> selectPermissions(@Param("enabledOnly") boolean enabledOnly, @Param("userId") Long userId);

    List<Map<String, Object>> selectConfigItems();

    List<Map<String, Object>> selectAttachments(@Param("limit") int limit, @Param("offset") int offset,
                                                @Param("storageType") String storageType);

    long countAttachments(@Param("storageType") String storageType);

    List<Map<String, Object>> selectLoginLogs();

    List<Map<String, Object>> selectOperationLogs();

    long countAdminUsers();

    long countRoles();

    long countDepartments();

    long countPermissions();

    List<String> selectConfigValue(@Param("key") String key);
}
