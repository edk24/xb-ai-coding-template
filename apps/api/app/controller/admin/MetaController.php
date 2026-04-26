<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use think\facade\Db;

class MetaController extends BaseController
{
    public function menuTree()
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        $query = Db::name('permissions')->where('status', 1);
        if ((int) $user->is_super !== 1) {
            $permissionIds = Db::name('admin_user_roles')
                ->alias('aur')
                ->join('role_permissions rp', 'rp.role_id = aur.role_id')
                ->where('aur.admin_user_id', (int) $user->id)
                ->column('rp.permission_id');
            $query->whereIn('id', array_unique($permissionIds ?: [0]));
        }

        $items = $query->order('sort asc,id asc')->select()->toArray();
        return json_success($this->buildTree($items));
    }

    public function dashboard()
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        return json_success([
            'welcome' => sprintf('欢迎回来，%s', $user->nickname),
            'stats' => [
                'admin_users' => Db::name('admin_users')->count(),
                'roles' => Db::name('roles')->count(),
                'departments' => Db::name('departments')->count(),
                'permissions' => Db::name('permissions')->count(),
            ],
        ]);
    }

    public function adminUsers()
    {
        $rows = Db::name('admin_users')
            ->alias('u')
            ->leftJoin('departments d', 'd.id = u.department_id')
            ->field('u.id,u.department_id,u.username,u.nickname,u.avatar,u.phone,u.email,u.status,u.is_super,u.remark,u.created_at,d.name as department_name')
            ->order('u.id asc')
            ->select()
            ->toArray();

        foreach ($rows as &$row) {
            $roles = Db::name('admin_user_roles')
                ->alias('aur')
                ->join('roles r', 'r.id = aur.role_id')
                ->where('aur.admin_user_id', (int) $row['id'])
                ->field('r.id,r.name')
                ->select()
                ->toArray();
            $row['roles'] = array_column($roles, 'name');
            $row['role_ids'] = array_map(static fn ($value) => (int) $value, array_column($roles, 'id'));
        }

        return json_success($rows);
    }

    public function roles()
    {
        $rows = Db::name('roles')->order('id asc')->select()->toArray();
        foreach ($rows as &$row) {
            $row['permission_ids'] = array_map(
                static fn ($value) => (int) $value,
                Db::name('role_permissions')->where('role_id', (int) $row['id'])->column('permission_id')
            );
        }
        return json_success($rows);
    }

    public function departments()
    {
        $rows = Db::name('departments')->order('sort asc,id asc')->select()->toArray();
        return json_success($this->buildTree($rows));
    }

    public function permissions()
    {
        $rows = Db::name('permissions')->order('sort asc,id asc')->select()->toArray();
        return json_success($this->buildTree($rows));
    }

    public function loginLogs()
    {
        $rows = Db::name('login_logs')->order('id desc')->limit(50)->select()->toArray();
        return json_success($rows);
    }

    public function operationLogs()
    {
        $rows = Db::name('operation_logs')->order('id desc')->limit(50)->select()->toArray();
        return json_success($rows);
    }

    private function buildTree(array $items, int $parentId = 0): array
    {
        $tree = [];
        foreach ($items as $item) {
            if ((int) ($item['parent_id'] ?? 0) !== $parentId) {
                continue;
            }

            $item['children'] = $this->buildTree($items, (int) $item['id']);
            $tree[] = $item;
        }

        return $tree;
    }
}
