<?php

declare(strict_types=1);

namespace app\service;

class StorageService
{
    /**
     * 获取当前存储驱动
     */
    public function getStorageType(): string
    {
        $dbDriver = \config_get('storage_driver');
        if ($dbDriver && in_array((string) $dbDriver, ['local', 'cos', 'oss'], true)) {
            return (string) $dbDriver;
        }
        return (string) config('storage.default', 'local');
    }

    /**
     * 获取存储配置（优先读取系统配置项，兼容 env/config 文件默认值）
     */
    public function getConfig(string $type = null): array
    {
        $type = $type ?? $this->getStorageType();
        $config = config('storage.disks.' . $type, []);

        // 从 DB 系统配置项中读取覆盖值（非空时覆盖文件配置）
        foreach ($config as $key => $default) {
            $dbValue = \config_get($type . '_' . $key);
            if ($dbValue !== null && $dbValue !== '') {
                $config[$key] = $dbValue;
            }
        }

        return $config;
    }

    /**
     * 获取 COS/OSS 临时上传凭证（前端直传用）
     */
    public function getCredentials(string $type): array
    {
        return match ($type) {
            'cos' => $this->getCosCredentials(),
            'oss' => $this->getOssCredentials(),
            default => throw new \InvalidArgumentException("不支持的存储类型: {$type}"),
        };
    }

    /**
     * 腾讯云 COS STS 临时凭证
     */
    private function getCosCredentials(): array
    {
        if (!class_exists('\Qcloud\Sts\Sts')) {
            // 尝试加载 cos-sdk-v5 中的 STS
            if (!class_exists('\Qcloud\Cos\Client', false)) {
                api_abort('COS SDK 未安装，请执行: composer require qcloud/cos-sdk-v5', 500);
            }
        }

        $config = $this->getConfig('cos');
        $sts = new \Qcloud\Sts\Sts();

        $bucket = $config['bucket'];
        $prefix = rtrim((string) $config['allow_prefix'], '/') . '/' . date('Y/m/d');

        $result = $sts->getTempKeys([
            'url' => 'https://sts.tencentcloudapi.com/',
            'domain' => 'sts.tencentcloudapi.com',
            'proxy' => '',
            'secretId' => (string) $config['secret_id'],
            'secretKey' => (string) $config['secret_key'],
            'bucket' => $bucket,
            'region' => (string) $config['region'],
            'durationSeconds' => (int) ($config['duration'] ?? 1800),
            'allowPrefix' => $prefix,
            'allowActions' => [
                'name/cos:PutObject',
                'name/cos:PostObject',
                'name/cos:InitiateMultipartUpload',
                'name/cos:ListParts',
                'name/cos:UploadPart',
                'name/cos:CompleteMultipartUpload',
            ],
        ]);

        return [
            'storage_type' => 'cos',
            'credentials' => [
                'tmpSecretId' => $result['credentials']['tmpSecretId'] ?? '',
                'tmpSecretKey' => $result['credentials']['tmpSecretKey'] ?? '',
                'sessionToken' => $result['credentials']['sessionToken'] ?? '',
            ],
            'region' => (string) $config['region'],
            'bucket' => $bucket,
            'path_prefix' => $prefix,
            'cdn_url' => (string) $config['cdn_url'],
            'expired_time' => $result['expiredTime'] ?? 0,
        ];
    }

    /**
     * 阿里云 OSS STS 临时凭证
     */
    private function getOssCredentials(): array
    {
        if (!class_exists('\AlibabaCloud\Sts\StsClient', false) && !class_exists('\AlibabaCloud\Sts\V20150401\StsApiResolver', false)) {
            api_abort('OSS STS SDK 未安装，请执行: composer require alibabacloud/sts', 500);
        }

        $config = $this->getConfig('oss');
        $prefix = rtrim((string) $config['allow_prefix'], '/') . '/' . date('Y/m/d');

        // 使用 AlibabaCloud\Sts 获取 AssumeRole
        $client = new \AlibabaCloud\Sts\StsClient([
            'regionId' => (string) $config['region'],
            'accessKeyId' => (string) $config['access_key_id'],
            'accessKeySecret' => (string) $config['access_key_secret'],
        ]);

        $sessionName = 'admin-' . date('YmdHis');
        $result = $client->assumeRole(
            (string) $config['role_arn'],
            $sessionName,
            [
                'Policy' => json_encode([
                    'Version' => '1',
                    'Statement' => [
                        [
                            'Effect' => 'Allow',
                            'Action' => [
                                'oss:PutObject',
                                'oss:PostObject',
                                'oss:InitiateMultipartUpload',
                                'oss:UploadPart',
                                'oss:CompleteMultipartUpload',
                            ],
                            'Resource' => ["acs:oss:*:*:{$config['bucket']}/{$prefix}/*"],
                        ],
                    ],
                ]),
                'DurationSeconds' => (int) ($config['duration'] ?? 1800),
            ]
        );

        $creds = $result['Credentials'] ?? $result['credentials'] ?? [];

        return [
            'storage_type' => 'oss',
            'credentials' => [
                'tmpSecretId' => $creds['AccessKeyId'] ?? $creds['accessKeyId'] ?? '',
                'tmpSecretKey' => $creds['AccessKeySecret'] ?? $creds['accessKeySecret'] ?? '',
                'sessionToken' => $creds['SecurityToken'] ?? $creds['securityToken'] ?? '',
            ],
            'region' => (string) $config['region'],
            'bucket' => (string) $config['bucket'],
            'endpoint' => (string) $config['endpoint'],
            'path_prefix' => $prefix,
            'cdn_url' => (string) $config['cdn_url'],
            'expired_time' => strtotime($creds['Expiration'] ?? '+1 hour'),
        ];
    }

    /**
     * 保存本地上传文件
     * @return array{path: string, url: string}
     */
    public function saveLocalFile(string $tmpPath, string $originalName): array
    {
        $disk = config('storage.disks.local');
        $root = rtrim((string) ($disk['root'] ?? app()->getRootPath() . 'public/storage/attachments'), '/');
        $urlBase = rtrim((string) ($disk['url'] ?? '/storage/attachments'), '/');

        // 按日期分目录
        $dateDir = date('Y/m/d');
        $saveDir = $root . '/' . $dateDir;

        // 生成唯一文件名
        $ext = pathinfo($originalName, PATHINFO_EXTENSION);
        $filename = date('His') . '_' . uniqid() . ($ext ? '.' . $ext : '');

        // 确保目录存在
        if (!is_dir($saveDir)) {
            mkdir($saveDir, 0755, true);
        }

        // 移动上传文件
        $destPath = $saveDir . '/' . $filename;
        move_uploaded_file($tmpPath, $destPath);

        return [
            'path' => $dateDir . '/' . $filename,
            'url' => $urlBase . '/' . $dateDir . '/' . $filename,
        ];
    }

    /**
     * 删除存储中的文件
     */
    public function deleteFile(string $storageType, string $path): void
    {
        match ($storageType) {
            'local' => $this->deleteLocalFile($path),
            'cos' => $this->deleteCosFile($path),
            'oss' => $this->deleteOssFile($path),
            default => null,
        };
    }

    private function deleteLocalFile(string $path): void
    {
        $disk = config('storage.disks.local');
        $fullPath = rtrim((string) ($disk['root'] ?? app()->getRootPath() . 'public/storage/attachments'), '/') . '/' . ltrim($path, '/');
        if (file_exists($fullPath)) {
            @unlink($fullPath);
        }
    }

    private function deleteCosFile(string $path): void
    {
        if (!class_exists('\Qcloud\Cos\Client', false)) {
            return;
        }

        $config = $this->getConfig('cos');
        $cosClient = new \Qcloud\Cos\Client([
            'region' => (string) $config['region'],
            'credentials' => [
                'secretId' => (string) $config['secret_id'],
                'secretKey' => (string) $config['secret_key'],
            ],
        ]);

        try {
            $cosClient->deleteObject([
                'Bucket' => $config['bucket'],
                'Key' => ltrim($path, '/'),
            ]);
        } catch (\Throwable) {
            // 删除失败静默处理
        }
    }

    private function deleteOssFile(string $path): void
    {
        if (!class_exists('\OSS\OssClient', false)) {
            return;
        }

        $config = $this->getConfig('oss');
        try {
            $ossClient = new \OSS\OssClient(
                (string) $config['access_key_id'],
                (string) $config['access_key_secret'],
                (string) $config['bucket'] . '.' . (string) $config['region'] . '.aliyuncs.com'
            );
            $ossClient->deleteObject((string) $config['bucket'], ltrim($path, '/'));
        } catch (\Throwable) {
            // 删除失败静默处理
        }
    }
}
