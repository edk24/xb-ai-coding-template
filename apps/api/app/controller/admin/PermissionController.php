<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\facade\Db;
use think\Request;

class PermissionController extends BaseController
{
    public function create(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertParentValid((int) $payload['parent_id'], $payload['type']);
        $this->assertKeyUnique($payload['permission_key']);

        $permissionId = (int) Db::name('permissions')->insertGetId([
            'parent_id' => (int) $payload['parent_id'],
            'name' => $payload['name'],
            'type' => $payload['type'],
            'route_path' => $payload['route_path'],
            'component_path' => $payload['component_path'],
            'permission_key' => $payload['permission_key'],
            'sort' => (int) $payload['sort'],
            'hidden' => (int) $payload['hidden'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'created_by' => (int) $operator->id,
            'updated_by' => (int) $operator->id,
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        $authService->recordOperationLog((int) $operator->id, '菜单权限', '新增权限', $request->method(), $request->pathinfo(), ['name' => $payload['name']], ['id' => $permissionId], $request->ip());

        return json_success(Db::name('permissions')->where('id', $permissionId)->find(), '权限已创建');
    }

    public function update(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $permission = Db::name('permissions')->where('id', $id)->find();
        if (!$permission) {
            api_abort('权限不存在', 404);
        }

        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        if ($id === 1 || $id === 2 || $id === 3) {
            if ($payload['permission_key'] !== $permission['permission_key']) {
                api_abort('内置权限标识不允许修改', 422);
            }
        }
        $this->assertParentValid((int) $payload['parent_id'], $payload['type'], $id);
        $this->assertKeyUnique($payload['permission_key'], $id);

        if ((int) $payload['parent_id'] === $id) {
            api_abort('父节点不能选择自己', 422);
        }

        Db::name('permissions')->where('id', $id)->update([
            'parent_id' => (int) $payload['parent_id'],
            'name' => $payload['name'],
            'type' => $payload['type'],
            'route_path' => $payload['route_path'],
            'component_path' => $payload['component_path'],
            'permission_key' => $payload['permission_key'],
            'sort' => (int) $payload['sort'],
            'hidden' => (int) $payload['hidden'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'updated_by' => (int) $operator->id,
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        $authService->recordOperationLog((int) $operator->id, '菜单权限', '编辑权限', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(Db::name('permissions')->where('id', $id)->find(), '权限已更新');
    }

    public function delete(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $permission = Db::name('permissions')->where('id', $id)->find();
        if (!$permission) {
            api_abort('权限不存在', 404);
        }
        if ($id <= 16) {
            api_abort('内置权限不允许删除', 422);
        }
        if (Db::name('permissions')->where('parent_id', $id)->count() > 0) {
            api_abort('存在子节点，不能删除', 422);
        }

        Db::transaction(function () use ($id) {
            Db::name('role_permissions')->where('permission_id', $id)->delete();
            Db::name('permissions')->where('id', $id)->delete();
        });

        $authService->recordOperationLog((int) $operator->id, '菜单权限', '删除权限', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(null, '权限已删除');
    }

    private function normalizePayload(Request $request): array
    {
        return [
            'parent_id' => (int) $request->param('parent_id', 0),
            'name' => trim((string) $request->param('name', '')),
            'type' => trim((string) $request->param('type', 'menu')),
            'route_path' => trim((string) $request->param('route_path', '')),
            'component_path' => trim((string) $request->param('component_path', '')),
            'permission_key' => trim((string) $request->param('permission_key', '')),
            'sort' => (int) $request->param('sort', 0),
            'hidden' => (string) ((int) $request->param('hidden', 0)),
            'status' => (string) ((int) $request->param('status', 1)),
            'remark' => trim((string) $request->param('remark', '')),
        ];
    }

    private function validatePayload(array $payload): void
    {
        $this->validate($payload, [
            'parent_id' => 'integer|egt:0',
            'name' => 'require|max:100',
            'type' => 'require|in:catalog,menu,button',
            'route_path' => 'max:255',
            'component_path' => 'max:255',
            'permission_key' => 'require|max:120',
            'sort' => 'integer|egt:0',
            'hidden' => 'require|in:0,1',
            'status' => 'require|in:0,1',
            'remark' => 'max:255',
        ]);
    }

    private function assertParentValid(int $parentId, string $type, ?int $ignoreId = null): void
    {
        if ($parentId === 0) {
            return;
        }
        $query = Db::name('permissions')->where('id', $parentId);
        if ($ignoreId) {
            $query->where('id', '<>', $ignoreId);
        }
        $parent = $query->find();
        if (!$parent) {
            api_abort('父级权限不存在', 422);
        }
        $parentType = (string) $parent['type'];
        if ($type === 'catalog' && $parentId !== 0) {
            api_abort('目录只能作为顶级节点', 422);
        }
        if ($type === 'menu' && $parentType !== 'catalog') {
            api_abort('菜单节点只能挂在目录下', 422);
        }
        if ($type === 'button' && $parentType !== 'menu') {
            api_abort('按钮节点只能挂在菜单下', 422);
        }
    }

    private function assertKeyUnique(string $permissionKey, ?int $ignoreId = null): void
    {
        $query = Db::name('permissions')->where('permission_key', $permissionKey);
        if ($ignoreId) {
            $query->where('id', '<>', $ignoreId);
        }
        if ($query->find()) {
            api_abort('权限标识已存在', 422);
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
}
