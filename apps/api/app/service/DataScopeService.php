<?php

declare(strict_types=1);

namespace app\service;

use think\facade\Db;

class DataScopeService
{
    /**
     * 获取当前用户的有效数据权限范围（取所有角色中的最大值）。
     *
     * @return int 1=仅本人 2=本部门 3=本部门及以下 4=全部
     */
    public function getEffectiveScope(): int
    {
        $user = current_admin_user();
        if (!$user) {
            return 1;
        }

        // 超级管理员直接取最大范围
        if ((int) $user->is_super === 1) {
            return 4;
        }

        $scope = Db::name('admin_user_roles')
            ->alias('aur')
            ->join('roles r', 'r.id = aur.role_id')
            ->where('aur.admin_user_id', (int) $user->id)
            ->max('r.data_scope');

        return $scope ? (int) $scope : 1;
    }

    /**
     * 获取当前用户基于数据权限可见的部门 ID 列表。
     *
     * @return int[] 部门 ID 数组；空数组表示不做部门级过滤
     */
    public function getVisibleDeptIds(): array
    {
        $scope = $this->getEffectiveScope();
        $user = current_admin_user();

        if (!$user || $scope >= 4) {
            return [];
        }

        if ($scope === 1) {
            return [];
        }

        $deptIds = [(int) $user->department_id];

        if ($scope >= 3) {
            $allDepts = Db::name('departments')->select()->toArray();
            $subIds = $this->getSubDeptIds($allDepts, (int) $user->department_id);
            $deptIds = array_merge($deptIds, $subIds);
        }

        return array_unique(array_map(static fn ($id) => (int) $id, $deptIds));
    }

    /**
     * 递归获取指定父级部门下的所有子部门 ID。
     */
    private function getSubDeptIds(array $allDepts, int $parentId): array
    {
        $ids = [];
        foreach ($allDepts as $dept) {
            if ((int) $dept['parent_id'] === $parentId) {
                $ids[] = (int) $dept['id'];
                $ids = array_merge($ids, $this->getSubDeptIds($allDepts, (int) $dept['id']));
            }
        }
        return $ids;
    }
}
