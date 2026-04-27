<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\facade\Db;
use think\Request;

class RoleController extends BaseController
{
    public function create(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertUnique($payload['name'], $payload['code']);
        $permissionIds = $this->normalizePermissionIds($payload['permission_ids']);
        $this->assertPermissionIds($permissionIds);

        $roleId = Db::transaction(function () use ($payload, $permissionIds, $operator) {
            $now = date('Y-m-d H:i:s');
            $roleId = (int) Db::name('roles')->insertGetId([
                'name' => $payload['name'],
                'code' => $payload['code'],
                'status' => (int) $payload['status'],
                'data_scope' => $payload['data_scope'],
                'remark' => $payload['remark'],
                'created_by' => (int) $operator->id,
                'updated_by' => (int) $operator->id,
                'created_at' => $now,
                'updated_at' => $now,
            ]);

            foreach ($permissionIds as $permissionId) {
                Db::name('role_permissions')->insert([
                    'role_id' => $roleId,
                    'permission_id' => $permissionId,
                ]);
            }

            return $roleId;
        });

        $authService->recordOperationLog((int) $operator->id, '角色管理', '新增角色', $request->method(), $request->pathinfo(), ['name' => $payload['name']], ['id' => $roleId], $request->ip());

        return json_success($this->findRow($roleId), '角色已创建');
    }

    public function update(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $role = Db::name('roles')->where('id', $id)->find();
        if (!$role) {
            api_abort('角色不存在', 404);
        }

        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertUnique($payload['name'], $payload['code'], $id);
        $permissionIds = $this->normalizePermissionIds($payload['permission_ids']);
        $this->assertPermissionIds($permissionIds);

        Db::transaction(function () use ($id, $payload, $permissionIds, $operator) {
            Db::name('roles')->where('id', $id)->update([
                'name' => $payload['name'],
                'code' => $payload['code'],
                'status' => (int) $payload['status'],
                'data_scope' => $payload['data_scope'],
                'remark' => $payload['remark'],
                'updated_by' => (int) $operator->id,
                'updated_at' => date('Y-m-d H:i:s'),
            ]);
            Db::name('role_permissions')->where('role_id', $id)->delete();
            foreach ($permissionIds as $permissionId) {
                Db::name('role_permissions')->insert([
                    'role_id' => $id,
                    'permission_id' => $permissionId,
                ]);
            }
        });

        $authService->recordOperationLog((int) $operator->id, '角色管理', '编辑角色', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success($this->findRow($id), '角色已更新');
    }

    public function delete(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $role = Db::name('roles')->where('id', $id)->find();
        if (!$role) {
            api_abort('角色不存在', 404);
        }
        if ((int) $id === 1) {
            api_abort('内置角色不允许删除', 422);
        }
        if (Db::name('admin_user_roles')->where('role_id', $id)->count() > 0) {
            api_abort('角色已绑定管理员，不能删除', 422);
        }

        Db::transaction(function () use ($id) {
            Db::name('role_permissions')->where('role_id', $id)->delete();
            Db::name('roles')->where('id', $id)->delete();
        });

        $authService->recordOperationLog((int) $operator->id, '角色管理', '删除角色', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(null, '角色已删除');
    }

    private function normalizePayload(Request $request): array
    {
        return [
            'name' => trim((string) $request->param('name', '')),
            'code' => trim((string) $request->param('code', '')),
            'status' => (string) ((int) $request->param('status', 1)),
            'data_scope' => (int) $request->param('data_scope', 2),
            'remark' => trim((string) $request->param('remark', '')),
            'permission_ids' => (array) ($request->param('permission_ids') ?? []),
        ];
    }

    private function validatePayload(array $payload): void
    {
        $this->validate($payload, [
            'name' => 'require|max:100',
            'code' => 'require|max:100',
            'status' => 'require|in:0,1',
            'data_scope' => 'require|in:1,2,3,4',
            'remark' => 'max:255',
        ]);
    }

    private function assertUnique(string $name, string $code, ?int $ignoreId = null): void
    {
        $nameQuery = Db::name('roles')->where('name', $name);
        $codeQuery = Db::name('roles')->where('code', $code);
        if ($ignoreId) {
            $nameQuery->where('id', '<>', $ignoreId);
            $codeQuery->where('id', '<>', $ignoreId);
        }
        if ($nameQuery->find()) {
            api_abort('角色名称已存在', 422);
        }
        if ($codeQuery->find()) {
            api_abort('角色编码已存在', 422);
        }
    }

    private function normalizePermissionIds(array $permissionIds): array
    {
        return array_values(array_unique(array_filter(array_map(static fn ($id) => (int) $id, $permissionIds))));
    }

    private function assertPermissionIds(array $permissionIds): void
    {
        if ($permissionIds === []) {
            api_abort('请至少选择一个权限节点', 422);
        }
        $count = Db::name('permissions')->whereIn('id', $permissionIds)->count();
        if ($count !== count($permissionIds)) {
            api_abort('权限数据无效', 422);
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
        $row = Db::name('roles')->where('id', $id)->find();
        if (!$row) {
            api_abort('角色不存在', 404);
        }
        $row['permission_ids'] = array_map(
            static fn ($value) => (int) $value,
            Db::name('role_permissions')->where('role_id', $id)->column('permission_id')
        );
        return $row;
    }
}
