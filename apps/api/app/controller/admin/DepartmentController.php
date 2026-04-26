<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\facade\Db;
use think\Request;

class DepartmentController extends BaseController
{
    public function create(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertParentExists((int) $payload['parent_id']);

        $departmentId = (int) Db::name('departments')->insertGetId([
            'parent_id' => (int) $payload['parent_id'],
            'name' => $payload['name'],
            'leader' => $payload['leader'],
            'phone' => $payload['phone'],
            'sort' => (int) $payload['sort'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'created_by' => (int) $operator->id,
            'updated_by' => (int) $operator->id,
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        $authService->recordOperationLog((int) $operator->id, '部门管理', '新增部门', $request->method(), $request->pathinfo(), ['name' => $payload['name']], ['id' => $departmentId], $request->ip());

        return json_success(Db::name('departments')->where('id', $departmentId)->find(), '部门已创建');
    }

    public function update(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $department = Db::name('departments')->where('id', $id)->find();
        if (!$department) {
            api_abort('部门不存在', 404);
        }

        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertParentExists((int) $payload['parent_id'], $id);

        if ((int) $id === (int) $payload['parent_id']) {
            api_abort('上级部门不能选择自己', 422);
        }

        Db::name('departments')->where('id', $id)->update([
            'parent_id' => (int) $payload['parent_id'],
            'name' => $payload['name'],
            'leader' => $payload['leader'],
            'phone' => $payload['phone'],
            'sort' => (int) $payload['sort'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'updated_by' => (int) $operator->id,
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        $authService->recordOperationLog((int) $operator->id, '部门管理', '编辑部门', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(Db::name('departments')->where('id', $id)->find(), '部门已更新');
    }

    public function delete(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $department = Db::name('departments')->where('id', $id)->find();
        if (!$department) {
            api_abort('部门不存在', 404);
        }
        if ((int) $id === 1) {
            api_abort('根部门不允许删除', 422);
        }
        if (Db::name('departments')->where('parent_id', $id)->count() > 0) {
            api_abort('存在子部门，不能删除', 422);
        }
        if (Db::name('admin_users')->where('department_id', $id)->count() > 0) {
            api_abort('部门下存在管理员，不能删除', 422);
        }

        Db::name('departments')->where('id', $id)->delete();

        $authService->recordOperationLog((int) $operator->id, '部门管理', '删除部门', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        return json_success(null, '部门已删除');
    }

    private function normalizePayload(Request $request): array
    {
        return [
            'parent_id' => (int) $request->param('parent_id', 0),
            'name' => trim((string) $request->param('name', '')),
            'leader' => trim((string) $request->param('leader', '')),
            'phone' => trim((string) $request->param('phone', '')),
            'sort' => (int) $request->param('sort', 0),
            'status' => (string) ((int) $request->param('status', 1)),
            'remark' => trim((string) $request->param('remark', '')),
        ];
    }

    private function validatePayload(array $payload): void
    {
        $this->validate($payload, [
            'parent_id' => 'integer|egt:0',
            'name' => 'require|max:100',
            'leader' => 'max:100',
            'phone' => 'max:32',
            'sort' => 'integer|egt:0',
            'status' => 'require|in:0,1',
            'remark' => 'max:255',
        ]);
    }

    private function assertParentExists(int $parentId, ?int $ignoreId = null): void
    {
        if ($parentId === 0) {
            return;
        }
        $query = Db::name('departments')->where('id', $parentId);
        if ($ignoreId) {
            $query->where('id', '<>', $ignoreId);
        }
        if (!$query->find()) {
            api_abort('上级部门不存在', 422);
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
