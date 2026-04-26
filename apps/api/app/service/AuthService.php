<?php

declare(strict_types=1);

namespace app\service;

use app\model\AdminUser;
use think\facade\Db;

class AuthService
{
    public function attempt(string $username, string $password, string $ip, string $userAgent): array
    {
        /** @var AdminUser|null $user */
        $user = AdminUser::where('username', $username)->find();

        if (!$user) {
            $this->recordLoginLog(null, $username, $ip, $userAgent, 0, '账号不存在');
            api_abort('账号或密码错误', 401);
        }

        if ((int) $user->status !== 1) {
            $this->recordLoginLog((int) $user->id, $username, $ip, $userAgent, 0, '账号已禁用');
            api_abort('账号已禁用', 403);
        }

        if (admin_password_hash($password, (string) $user->salt) !== $user->password_hash) {
            $this->recordLoginLog((int) $user->id, $username, $ip, $userAgent, 0, '密码错误');
            api_abort('账号或密码错误', 401);
        }

        $token = create_admin_token($user);

        $user->last_login_at = date('Y-m-d H:i:s');
        $user->last_login_ip = $ip;
        $user->save();

        $this->recordLoginLog((int) $user->id, $username, $ip, $userAgent, 1, '');

        return [
            'token' => $token,
            'expire_in' => jwt_expire(),
            'user' => $this->buildProfile($user),
        ];
    }

    public function buildProfile(AdminUser $user): array
    {
        $roles = Db::name('admin_user_roles')
            ->alias('aur')
            ->join('roles r', 'r.id = aur.role_id')
            ->where('aur.admin_user_id', (int) $user->id)
            ->column('r.name');

        $permissionKeys = [];
        if ((int) $user->is_super === 1) {
            $permissionKeys = Db::name('permissions')->column('permission_key');
        } else {
            $permissionKeys = Db::name('admin_user_roles')
                ->alias('aur')
                ->join('role_permissions rp', 'rp.role_id = aur.role_id')
                ->join('permissions p', 'p.id = rp.permission_id')
                ->where('aur.admin_user_id', (int) $user->id)
                ->distinct(true)
                ->column('p.permission_key');
        }

        $department = Db::name('departments')
            ->where('id', (int) $user->department_id)
            ->value('name');

        return [
            'id' => (int) $user->id,
            'username' => $user->username,
            'nickname' => $user->nickname,
            'avatar' => $user->avatar,
            'phone' => $user->phone,
            'email' => $user->email,
            'remark' => $user->remark,
            'department_name' => $department ?: '',
            'roles' => array_values($roles),
            'permission_keys' => array_values(array_unique($permissionKeys)),
            'is_super' => (int) $user->is_super === 1,
        ];
    }

    public function recordOperationLog(?int $adminUserId, string $module, string $action, string $method, string $path, array $requestData, array $responseData, string $ip): void
    {
        Db::name('operation_logs')->insert([
            'admin_user_id' => $adminUserId,
            'module' => $module,
            'action' => $action,
            'method' => $method,
            'path' => $path,
            'request_summary' => json_encode($requestData, JSON_UNESCAPED_UNICODE),
            'response_summary' => json_encode($responseData, JSON_UNESCAPED_UNICODE),
            'ip' => $ip,
            'operated_at' => date('Y-m-d H:i:s'),
        ]);
    }

    private function recordLoginLog(?int $adminUserId, string $username, string $ip, string $userAgent, int $status, string $failReason): void
    {
        Db::name('login_logs')->insert([
            'admin_user_id' => $adminUserId,
            'username_snapshot' => $username,
            'ip' => $ip,
            'user_agent' => mb_substr($userAgent, 0, 255),
            'status' => $status,
            'fail_reason' => $failReason,
            'login_at' => date('Y-m-d H:i:s'),
        ]);
    }
}
