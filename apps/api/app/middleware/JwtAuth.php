<?php

declare(strict_types=1);

namespace app\middleware;

use app\model\AdminUser;
use Closure;
use Firebase\JWT\ExpiredException;
use Firebase\JWT\SignatureInvalidException;
use think\exception\HttpResponseException;
use think\Request;

class JwtAuth
{
    public function handle(Request $request, Closure $next)
    {
        $header = (string) $request->header('authorization', '');
        if (!str_starts_with($header, 'Bearer ')) {
            throw new HttpResponseException(json_error('未登录或登录已过期', 401));
        }

        $token = trim(substr($header, 7));

        try {
            $payload = parse_admin_token($token);
        } catch (ExpiredException|SignatureInvalidException $exception) {
            throw new HttpResponseException(json_error('登录状态已失效', 401));
        } catch (\Throwable $exception) {
            throw new HttpResponseException(json_error('无效的登录凭证', 401));
        }

        $user = AdminUser::find((int) ($payload['uid'] ?? 0));
        if (!$user || (int) $user->status !== 1) {
            throw new HttpResponseException(json_error('当前账号不可用', 401));
        }

        app()->instance('currentAdminUser', $user);
        app()->instance('currentAdminPayload', $payload);

        return $next($request);
    }
}
