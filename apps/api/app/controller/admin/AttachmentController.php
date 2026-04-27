<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use app\service\DataScopeService;
use app\service\StorageService;
use think\Request;
use think\facade\Db;

class AttachmentController extends BaseController
{
    public function config(StorageService $storageService)
    {
        $storageType = $storageService->getStorageType();
        $diskConfig = $storageService->getConfig();

        return json_success([
            'storage_type' => $storageType,
            'max_file_size' => (int) env('ATTACHMENT_MAX_SIZE', 20971520),
            'allowed_extensions' => env('ATTACHMENT_ALLOWED_EXTENSIONS', 'jpg,jpeg,png,gif,bmp,webp,doc,docx,xls,xlsx,pdf,txt,zip,rar,mp4,avi'),
        ]);
    }

    public function index(Request $request)
    {
        $page = max(1, (int) $request->get('page', 1));
        $pageSize = min(100, max(1, (int) $request->get('page_size', 20)));
        $storageType = (string) $request->get('storage_type', '');

        $query = Db::name('attachments')
            ->alias('a')
            ->leftJoin('admin_users u', 'u.id = a.admin_user_id')
            ->field('a.*')
            ->order('a.id desc');

        if ($storageType !== '' && in_array($storageType, ['local', 'cos', 'oss'])) {
            $query->where('a.storage_type', $storageType);
        }

        $dataScopeService = new DataScopeService();
        $scope = $dataScopeService->getEffectiveScope();
        if ($scope === 1) {
            $user = current_admin_user();
            $query->where('a.admin_user_id', $user ? (int) $user->id : 0);
        } elseif ($scope === 2 || $scope === 3) {
            $deptIds = $dataScopeService->getVisibleDeptIds();
            if (!empty($deptIds)) {
                $query->whereIn('u.department_id', $deptIds);
            }
        }

        $total = $query->count();
        $rows = $query->page($page, $pageSize)->select()->toArray();

        return json_success([
            'items' => $rows,
            'total' => $total,
            'page' => $page,
            'page_size' => $pageSize,
        ]);
    }

    public function upload(Request $request, StorageService $storageService, AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        // 直接使用 $_FILES，绕过 $request->file() 在 ThinkPHP 异常处理中的重入问题
        $rawFile = $_FILES['file'] ?? null;
        if (!$rawFile || $rawFile['error'] !== UPLOAD_ERR_OK) {
            api_abort('请选择要上传的文件', 400);
        }

        $uploadResult = $storageService->saveLocalFile($rawFile['tmp_name'], $rawFile['name']);

        $attachment = [
            'admin_user_id' => (int) $user->id,
            'name' => $rawFile['name'],
            'size' => $rawFile['size'],
            'mime_type' => $rawFile['type'] ?: '',
            'storage_type' => 'local',
            'path' => $uploadResult['path'],
            'url' => $uploadResult['url'],
        ];

        $attachmentId = Db::name('attachments')->insertGetId($attachment);
        $attachment['id'] = $attachmentId;

        $authService->recordOperationLog(
            (int) $user->id, '附件中心', '上传附件',
            $request->method(), $request->pathinfo(),
            ['name' => $attachment['name'], 'size' => $attachment['size']],
            ['success' => true], $request->ip()
        );

        return json_success($attachment, '上传成功');
    }

    public function credentials(Request $request, StorageService $storageService)
    {
        $payload = $request->only(['storage_type']);
        $type = trim((string) ($payload['storage_type'] ?? ''));

        if (!in_array($type, ['cos', 'oss'])) {
            api_abort('不支持的存储类型，仅支持 cos/oss', 400);
        }

        try {
            $result = $storageService->getCredentials($type);
            return json_success($result);
        } catch (\InvalidArgumentException $e) {
            api_abort($e->getMessage(), 400);
        }
    }

    public function record(Request $request, AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        $payload = $request->only(['name', 'size', 'mime_type', 'storage_type', 'path', 'url', 'cdn_url']);
        $this->validate($payload, [
            'name' => 'require|max:255',
            'size' => 'require|integer',
            'storage_type' => 'require|in:cos,oss',
            'path' => 'require|max:500',
        ]);

        $url = trim((string) ($payload['url'] ?? ''));
        // 如果没有 url，用 cdn_url + path 拼接
        if ($url === '') {
            $cdnUrl = rtrim((string) ($payload['cdn_url'] ?? ''), '/');
            $url = $cdnUrl ? $cdnUrl . '/' . ltrim((string) $payload['path'], '/') : '';
        }

        $attachment = [
            'admin_user_id' => (int) $user->id,
            'name' => trim((string) $payload['name']),
            'size' => (int) $payload['size'],
            'mime_type' => trim((string) ($payload['mime_type'] ?? '')),
            'storage_type' => trim((string) $payload['storage_type']),
            'path' => trim((string) $payload['path']),
            'url' => $url,
        ];

        $attachmentId = Db::name('attachments')->insertGetId($attachment);
        $attachment['id'] = $attachmentId;

        $authService->recordOperationLog(
            (int) $user->id, '附件中心', '上传附件',
            $request->method(), $request->pathinfo(),
            ['name' => $attachment['name'], 'size' => $attachment['size'], 'storage_type' => $attachment['storage_type']],
            ['success' => true], $request->ip()
        );

        return json_success($attachment, '记录成功');
    }

    public function delete(Request $request, StorageService $storageService, AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        $id = (int) $request->param('id', '0');
        $attachment = Db::name('attachments')->find($id);
        if (!$attachment) {
            api_abort('附件不存在', 404);
        }

        // 删除存储文件
        $storageService->deleteFile((string) $attachment['storage_type'], (string) $attachment['path']);

        Db::name('attachments')->delete($id);

        $authService->recordOperationLog(
            (int) $user->id, '附件中心', '删除附件',
            $request->method(), $request->pathinfo(),
            ['id' => $id, 'name' => $attachment['name']],
            ['success' => true], $request->ip()
        );

        return json_success(null, '删除成功');
    }
}
