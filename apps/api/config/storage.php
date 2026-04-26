<?php
// 附件存储配置
return [
    // 默认存储驱动: local | cos | oss
    'default' => env('STORAGE_DRIVER', 'local'),

    // 允许的存储类型列表（用于前端选择）
    'allow_types' => ['local', 'cos', 'oss'],

    'disks' => [
        'local' => [
            'root' => app()->getRootPath() . 'public/storage/attachments',
            'url'  => '/storage/attachments',
        ],

        'cos' => [
            'region'       => env('COS_REGION', 'ap-guangzhou'),
            'bucket'       => env('COS_BUCKET', ''),
            'secret_id'    => env('COS_SECRET_ID', ''),
            'secret_key'   => env('COS_SECRET_KEY', ''),
            'app_id'       => env('COS_APP_ID', ''),
            'cdn_url'      => env('COS_CDN_URL', ''),
            'allow_prefix' => env('COS_ALLOW_PREFIX', 'uploads'),
            'duration'     => (int) env('COS_CREDENTIALS_DURATION', 1800),
        ],

        'oss' => [
            'region'            => env('OSS_REGION', 'oss-cn-hangzhou'),
            'bucket'            => env('OSS_BUCKET', ''),
            'access_key_id'     => env('OSS_ACCESS_KEY_ID', ''),
            'access_key_secret' => env('OSS_ACCESS_KEY_SECRET', ''),
            'role_arn'          => env('OSS_ROLE_ARN', ''),
            'endpoint'          => env('OSS_ENDPOINT', ''),
            'cdn_url'           => env('OSS_CDN_URL', ''),
            'allow_prefix'      => env('OSS_ALLOW_PREFIX', 'uploads'),
            'duration'          => (int) env('OSS_CREDENTIALS_DURATION', 1800),
        ],
    ],
];
