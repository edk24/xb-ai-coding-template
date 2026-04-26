<?php

declare(strict_types=1);

namespace app\controller\admin;

use app\BaseController;
use app\service\AuthService;
use think\Request;

class AuthController extends BaseController
{
    public function login(Request $request, AuthService $authService)
    {
        $payload = $request->only(['username', 'password']);
        $this->validate($payload, [
            'username' => 'require|max:100',
            'password' => 'require|max:100',
        ]);

        $result = $authService->attempt(
            trim((string) $payload['username']),
            (string) $payload['password'],
            $request->ip(),
            (string) $request->header('user-agent', '')
        );

        return json_success($result, '登录成功');
    }

    public function logout()
    {
        return json_success(null, '退出成功');
    }

    public function profile(AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        return json_success($authService->buildProfile($user));
    }

    public function updateProfile(Request $request, AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        $payload = $request->only(['nickname', 'avatar', 'phone', 'email', 'remark']);
        $payload['nickname'] = trim((string) ($payload['nickname'] ?? ''));
        $payload['avatar'] = trim((string) ($payload['avatar'] ?? ''));
        $payload['phone'] = trim((string) ($payload['phone'] ?? ''));
        $payload['email'] = trim((string) ($payload['email'] ?? ''));
        $payload['remark'] = trim((string) ($payload['remark'] ?? ''));

        $rules = [
            'nickname' => 'require|max:100',
            'avatar' => 'max:255',
            'phone' => 'max:32',
            'remark' => 'max:255',
        ];
        if ($payload['email'] !== '') {
            $rules['email'] = 'email|max:120';
        }
        $this->validate($payload, $rules);

        $user->save($payload);
        $authService->recordOperationLog((int) $user->id, '个人资料', '更新资料', $request->method(), $request->pathinfo(), $payload, ['success' => true], $request->ip());

        return json_success($authService->buildProfile($user), '资料已更新');
    }

    public function updatePassword(Request $request, AuthService $authService)
    {
        $user = current_admin_user();
        if (!$user) {
            api_abort('用户不存在', 404);
        }

        $payload = $request->only(['old_password', 'new_password', 'confirm_password']);
        $this->validate($payload, [
            'old_password' => 'require|max:100',
            'new_password' => 'require|min:6|max:100',
            'confirm_password' => 'require|max:100',
        ]);

        if ($payload['new_password'] !== $payload['confirm_password']) {
            api_abort('两次输入的新密码不一致', 422);
        }

        if (admin_password_hash((string) $payload['old_password'], (string) $user->salt) !== $user->password_hash) {
            api_abort('原密码错误', 422);
        }

        $user->password_hash = admin_password_hash((string) $payload['new_password'], (string) $user->salt);
        $user->save();

        $authService->recordOperationLog((int) $user->id, '个人资料', '修改密码', $request->method(), $request->pathinfo(), ['id' => (int) $user->id], ['success' => true], $request->ip());

        return json_success(null, '密码修改成功，请重新登录');
    }
}
