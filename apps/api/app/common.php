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

function current_admin_user(): ?AdminUser
{
    try {
        $user = app()->make('currentAdminUser');
        return $user instanceof AdminUser ? $user : null;
    } catch (Throwable) {
        return null;
    }
}
