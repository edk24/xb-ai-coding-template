<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\facade\Cache;
use think\facade\Db;
use think\Request;

class ConfigItemController extends BaseController
{
    public function create(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertUniqueKey($payload['key']);

        $now = date('Y-m-d H:i:s');
        $options = $payload['options'] ? json_decode($payload['options'], true) : null;
        $id = (int) Db::name('config_items')->insertGetId([
            'name' => $payload['name'],
            'group_name' => $payload['group_name'],
            'key' => $payload['key'],
            'value' => $payload['value'],
            'type' => $payload['type'],
            'options' => $options ? json_encode($options, JSON_UNESCAPED_UNICODE) : '',
            'sort' => (int) $payload['sort'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'created_by' => (int) $operator->id,
            'updated_by' => (int) $operator->id,
            'created_at' => $now,
            'updated_at' => $now,
        ]);

        $authService->recordOperationLog((int) $operator->id, '系统配置', '新增配置项', $request->method(), $request->pathinfo(), ['key' => $payload['key']], ['id' => $id], $request->ip());

        Cache::delete('system_config_items');

        return json_success(Db::name('config_items')->where('id', $id)->find(), '配置项已创建');
    }

    public function update(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $item = Db::name('config_items')->where('id', $id)->find();
        if (!$item) {
            api_abort('配置项不存在', 404);
        }

        $payload = $this->normalizePayload($request);
        $this->validatePayload($payload);
        $this->assertUniqueKey($payload['key'], $id);

        $options = $payload['options'] ? json_decode($payload['options'], true) : null;
        Db::name('config_items')->where('id', $id)->update([
            'name' => $payload['name'],
            'group_name' => $payload['group_name'],
            'key' => $payload['key'],
            'value' => $payload['value'],
            'type' => $payload['type'],
            'options' => $options ? json_encode($options, JSON_UNESCAPED_UNICODE) : '',
            'sort' => (int) $payload['sort'],
            'status' => (int) $payload['status'],
            'remark' => $payload['remark'],
            'updated_by' => (int) $operator->id,
            'updated_at' => date('Y-m-d H:i:s'),
        ]);

        $authService->recordOperationLog((int) $operator->id, '系统配置', '编辑配置项', $request->method(), $request->pathinfo(), ['id' => $id], ['success' => true], $request->ip());

        Cache::delete('system_config_items');

        return json_success(Db::name('config_items')->where('id', $id)->find(), '配置项已更新');
    }

    public function delete(Request $request, AuthService $authService, int $id)
    {
        $operator = $this->requireOperator();
        $item = Db::name('config_items')->where('id', $id)->find();
        if (!$item) {
            api_abort('配置项不存在', 404);
        }

        Db::name('config_items')->where('id', $id)->delete();

        $authService->recordOperationLog((int) $operator->id, '系统配置', '删除配置项', $request->method(), $request->pathinfo(), ['key' => $item['key']], ['success' => true], $request->ip());

        Cache::delete('system_config_items');

        return json_success(null, '配置项已删除');
    }

    public function batchSave(Request $request, AuthService $authService)
    {
        $operator = $this->requireOperator();
        $items = (array) $request->param('items', []);

        foreach ($items as $item) {
            $id = (int) ($item['id'] ?? 0);
            $value = $item['value'] ?? '';
            if ($id <= 0) {
                continue;
            }
            $row = Db::name('config_items')->where('id', $id)->find();
            if (!$row) {
                continue;
            }
            $stored = is_array($value) ? json_encode($value, JSON_UNESCAPED_UNICODE) : (string) $value;
            Db::name('config_items')->where('id', $id)->update([
                'value' => $stored,
                'updated_by' => (int) $operator->id,
                'updated_at' => date('Y-m-d H:i:s'),
            ]);
        }

        $authService->recordOperationLog((int) $operator->id, '系统配置', '批量保存配置值', $request->method(), $request->pathinfo(), ['count' => count($items)], ['success' => true], $request->ip());

        Cache::delete('system_config_items');

        return json_success(null, '配置已保存');
    }

    private function normalizePayload(Request $request): array
    {
        return [
            'name' => trim((string) $request->param('name', '')),
            'group_name' => trim((string) $request->param('group_name', '')),
            'key' => trim((string) $request->param('key', '')),
            'value' => $request->param('value', ''),
            'type' => trim((string) $request->param('type', 'input')),
            'options' => trim((string) $request->param('options', '')),
            'sort' => (string) ((int) $request->param('sort', 0)),
            'status' => (string) ((int) $request->param('status', 1)),
            'remark' => trim((string) $request->param('remark', '')),
        ];
    }

    private function validatePayload(array $payload): void
    {
        $this->validate($payload, [
            'name' => 'require|max:100',
            'group_name' => 'max:100',
            'key' => 'require|max:120|regex:/^[a-z][a-zA-Z0-9_]+$/',
            'type' => 'require|in:input,textarea,number,select,radio,checkbox,image,multi_image,rich_text,switch,color',
            'sort' => 'number',
            'status' => 'require|in:0,1',
            'remark' => 'max:255',
        ]);
    }

    private function assertUniqueKey(string $key, ?int $ignoreId = null): void
    {
        $query = Db::name('config_items')->where('key', $key);
        if ($ignoreId) {
            $query->where('id', '<>', $ignoreId);
        }
        if ($query->find()) {
            api_abort('配置项键名已存在', 422);
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
