<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\facade\Db;
use think\Request;

class AdminUserController extends BaseController
{
    public function create(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $payload = $this->normalizePayload($request, true);

        $this->validate($payload, [
            'department_id' => 'require|integer|gt:0',
            'username' => 'require|max:100',
            'nickname' => 'require|max:100',
            'phone' => 'max:32',
            'status' => 'require|in:0,1',
            'remark' => 'max:255',
            'password' => 'require|min:6|max:100',
        ]);
        $this->validateEmail($payload['email']);
        $this->assertDepartmentExists((int) $payload['department_id']);
        $roleIds = $this->normalizeRoleIds($payload['role_ids']);
        $this->assertRoleIds($roleIds);

        if (Db::name('admin_users')->where('username', $payload['username'])->find()) {
            api_abort('账号已存在', 422);
        }

        $userId = Db::transaction(function () use ($payload, $roleIds, $operator) {
            $salt = bin2hex(random_bytes(4));
            $now = date('Y-m-d H:i:s');
            $userId = (int) Db::name('admin_users')->insertGetId([
                'department_id' => (int) $payload['department_id'],
                'username' => $payload['username'],
                'nickname' => $payload['nickname'],
                'avatar' => $payload['avatar'],
                'phone' => $payload['phone'],
                'email' => $payload['email'],
                'password_hash' => admin_password_hash($payload['password'], $salt),
                'salt' => $salt,
                'status' => (int) $payload['status'],
                'is_super' => 0,
                'remark' => $payload['remark'],
                'created_by' => (int) $operator->id,
                'updated_by' => (int) $operator->id,
                'created_at' => $now,
                'updated_at' => $now,
            ]);

            foreach ($roleIds as $roleId) {
                Db::name('admin_user_roles')->insert([
                    'admin_user_id' => $userId,
                    'role_id' => $roleId,
                ]);
            }

            return $userId;
        });

        $authService->recordOperationLog((int) $operator->id, '管理员管理', '新增管理员', $request->method(), $request->pathinfo(), ['username' => $payload['username']], ['id' => $userId], $request->ip());

        return json_success($this->findRow($userId), '管理员已创建');
    }

    public function update(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $user = Db::name('admin_users')->where('id', $id)->find();
        if (!$user) {
            api_abort('管理员不存在', 404);
        }

        $payload = $this->normalizePayload($request, false);
        $this->validate($payload, [
            'department_id' => 'require|integer|gt:0',
            'username' => 'require|max:100',
            'nickname' => 'require|max:100',
            'phone' => 'max:32',
            'status' => 'require|in:0,1',
            'remark' => 'max:255',
        ]);
        $this->validateEmail($payload['email']);
        $this->assertDepartmentExists((int) $payload['department_id']);
        $roleIds = $this->normalizeRoleIds($payload['role_ids']);
        $this->assertRoleIds($roleIds);

        $duplicate = Db::name('admin_users')
            ->where('username', $payload['username'])
            ->where('id', '<>', $id)
            ->find();
        if ($duplicate) {
            api_abort('账号已存在', 422);
        }

        if ((int) $user['is_super'] === 1 && (int) $payload['status'] !== 1) {
            api_abort('超级管理员不允许禁用', 422);
        }

        Db::transaction(function () use ($id, $payload, $roleIds, $operator) {
            Db::name('admin_users')
                ->where('id', $id)
                ->update([
                    'department_id' => (int) $payload['department_id'],
                    'username' => $payload['username'],
                    'nickname' => $payload['nickname'],
                    'avatar' => $payload['avatar'],
                    'phone' => $payload['phone'],
                    'email' => $payload['email'],
                    'status' => (int) $payload['status'],
                    'remark' => $payload['remark'],
                    'updated_by' => (int) $operator->id,
                    'updated_at' => date('Y-m-d H:i:s'),
                ]);

            Db::name('admin_user_roles')->where('admin_user_id', $id)->delete();
            foreach ($roleIds as $roleId) {
                Db::name('admin_user_roles')->insert([
                    'admin_user_id' => $id,
                    'role_id' => $roleId,
                ]);
            }
        });

        $authService->recordOperationLog((int) $operator->id, '管理员管理', '编辑管理员', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success($this->findRow($id), '管理员已更新');
    }

    public function delete(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $user = Db::name('admin_users')->where('id', $id)->find();
        if (!$user) {
            api_abort('管理员不存在', 404);
        }
        if ((int) $user['is_super'] === 1) {
            api_abort('超级管理员不允许删除', 422);
        }
        if ((int) $operator->id === $id) {
            api_abort('不能删除当前登录账号', 422);
        }

        Db::transaction(function () use ($id) {
            Db::name('admin_user_roles')->where('admin_user_id', $id)->delete();
            Db::name('admin_users')->where('id', $id)->delete();
        });

        $authService->recordOperationLog((int) $operator->id, '管理员管理', '删除管理员', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(null, '管理员已删除');
    }

    public function resetPassword(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $user = Db::name('admin_users')->where('id', $id)->find();
        if (!$user) {
            api_abort('管理员不存在', 404);
        }
        if ((int) $user['is_super'] === 1) {
            api_abort('超级管理员不允许重置', 422);
        }

        $newPassword = trim((string) $request->param('new_password', '123456'));
        if ($newPassword === '' || strlen($newPassword) < 6) {
            api_abort('新密码长度不能少于 6 位', 422);
        }

        Db::name('admin_users')
            ->where('id', $id)
            ->update([
                'password_hash' => admin_password_hash($newPassword, (string) $user['salt']),
                'updated_by' => (int) $operator->id,
                'updated_at' => date('Y-m-d H:i:s'),
            ]);

        $authService->recordOperationLog((int) $operator->id, '管理员管理', '重置密码', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(['new_password' => $newPassword], '密码已重置');
    }

    private function normalizePayload(Request $request, bool $creating): array
    {
        return [
            'department_id' => (int) $request->param('department_id', 0),
            'username' => trim((string) $request->param('username', '')),
            'nickname' => trim((string) $request->param('nickname', '')),
            'avatar' => trim((string) $request->param('avatar', '')),
            'phone' => trim((string) $request->param('phone', '')),
            'email' => trim((string) $request->param('email', '')),
            'status' => (string) ((int) $request->param('status', 1)),
            'remark' => trim((string) $request->param('remark', '')),
            'password' => $creating ? (string) $request->param('password', '') : '',
            'role_ids' => (array) ($request->param('role_ids') ?? []),
        ];
    }

    private function normalizeRoleIds(array $roleIds): array
    {
        $items = array_values(array_unique(array_filter(array_map(static fn ($id) => (int) $id, $roleIds))));
        return $items;
    }

    private function assertRoleIds(array $roleIds): void
    {
        if ($roleIds === []) {
            api_abort('请至少选择一个角色', 422);
        }
        $count = Db::name('roles')->whereIn('id', $roleIds)->count();
        if ($count !== count($roleIds)) {
            api_abort('角色数据无效', 422);
        }
    }

    private function assertDepartmentExists(int $departmentId): void
    {
        if (!Db::name('departments')->where('id', $departmentId)->find()) {
            api_abort('部门不存在', 422);
        }
    }

    private function validateEmail(string $email): void
    {
        if ($email !== '') {
            $this->validate(['email' => $email], ['email' => 'email|max:120']);
        }
    }

    private function requireOperator()
    {
        $operator = current_admin_user();
        if (!$operator) {
            api_abort('用户不存在', 404);
        }
        return $operator;
    }

    private function findRow(int $id): array
    {
        $row = Db::name('admin_users')
            ->alias('u')
            ->leftJoin('departments d', 'd.id = u.department_id')
            ->field('u.id,u.department_id,u.username,u.nickname,u.avatar,u.phone,u.email,u.status,u.is_super,u.remark,u.created_at,d.name as department_name')
            ->where('u.id', $id)
            ->find();
        if (!$row) {
            api_abort('管理员不存在', 404);
        }

        $roles = Db::name('admin_user_roles')
            ->alias('aur')
            ->join('roles r', 'r.id = aur.role_id')
            ->where('aur.admin_user_id', $id)
            ->field('r.id,r.name')
            ->select()
            ->toArray();
        $row['roles'] = array_column($roles, 'name');
        $row['role_ids'] = array_map(static fn ($value) => (int) $value, array_column($roles, 'id'));

        return $row;
    }
}
