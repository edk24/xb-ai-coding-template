<?php

declare(strict_types=1);

use app\model\AdminUser;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use think\exception\HttpResponseException;

function json_success(mixed $data = null, string $message = 'ok', int $code = 0)
{
    return json([
        'code' => $code,
        'message' => $message,
        'data' => $data,
    ]);
}

function json_error(string $message = '请求失败', int $code = 1, mixed $data = null)
{
    return json([
        'code' => $code,
        'message' => $message,
        'data' => $data,
    ]);
}

function api_abort(string $message, int $code = 1, mixed $data = null): never
{
    throw new HttpResponseException(json_error($message, $code, $data));
}

function admin_password_hash(string $password, string $salt): string
{
    return md5($password . $salt);
}

function jwt_secret(): string
{
    return env('JWT_SECRET', 'admin-system-jwt-secret-2026-for-hs256-minimum-length');
}

function jwt_expire(): int
{
    return (int) env('JWT_EXPIRE', 7200);
}

function create_admin_token(AdminUser $user): string
{
    $now = time();
    $payload = [
        'iss' => 'admin-system',
        'sub' => (string) $user->id,
        'uid' => (int) $user->id,
        'username' => $user->username,
        'is_super' => (int) $user->is_super,
        'iat' => $now,
        'exp' => $now + jwt_expire(),
    ];

    return JWT::encode($payload, jwt_secret(), 'HS256');
}

function parse_admin_token(string $token): array
{
    return (array) JWT::decode($token, new Key(jwt_secret(), 'HS256'));
}

/**
 * 获取系统配置值（多级缓存：请求级静态缓存 → TP 文件缓存 → 数据库）
 */
function config_get(string $key, mixed $default = null): mixed
{
    static $configs = null;

    if ($configs === null) {
        // 1. 尝试从 TP 缓存读取（跨请求共享）
        try {
            $cached = cache('system_config_items');
        } catch (\Throwable) {
            $cached = null;
        }

        if ($cached && is_array($cached)) {
            $configs = $cached;
        } else {
            // 2. 缓存未命中，从数据库加载
            try {
                $items = \think\facade\Db::name('config_items')
                    ->where('status', 1)
                    ->select()
                    ->toArray();
            } catch (\Throwable) {
                return $default;
            }

            $configs = [];
            foreach ($items as $item) {
                $configs[$item['key']] = $item;
            }

            // 3. 写入 TP 缓存，有效期 1 小时
            try {
                cache('system_config_items', $configs, 3600);
            } catch (\Throwable) {
                // 缓存写入失败不影响主流程
            }
        }
    }

    if (!isset($configs[$key])) {
        return $default;
    }

    $cfg = $configs[$key];
    $value = $cfg['value'] ?? '';
    $type = $cfg['type'] ?? 'input';

    return match ($type) {
        'number' => $value !== '' ? (float) $value : $default,
        'checkbox', 'multi_image' => $value ? (json_decode($value, true) ?? []) : ($default !== null ? $default : []),
        'switch' => $value !== '' ? (int) $value : $default,
        default => $value !== '' ? $value : $default,
    };
}

function current_admin_user(): ?AdminUser
{
    try {
        $user = app()->make('currentAdminUser');
        return $user instanceof AdminUser ? $user : null;
    } catch (Throwable) {
        return null;
    }
}
