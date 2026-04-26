<?php

declare(strict_types=1);

use app\controller\admin\AdminUserController;
use app\controller\admin\AttachmentController;
use app\controller\admin\AuthController;
use app\controller\admin\ConfigItemController;
use app\controller\admin\DepartmentController;
use app\controller\admin\MetaController;
use app\controller\admin\PermissionController;
use app\controller\admin\RoleController;
use think\facade\Route;

Route::get('/', function () {
    return json_success([
        'name' => 'ThinkPHP 8 Admin API',
        'status' => 'ok',
    ]);
});

Route::group('admin-api', function () {
    Route::post('auth/login', [AuthController::class, 'login']);

    Route::group('', function () {
        Route::post('auth/logout', [AuthController::class, 'logout']);
        Route::get('auth/profile', [AuthController::class, 'profile']);
        Route::put('auth/profile', [AuthController::class, 'updateProfile']);
        Route::put('auth/password', [AuthController::class, 'updatePassword']);

        Route::get('meta/dashboard', [MetaController::class, 'dashboard']);
        Route::get('meta/menu-tree', [MetaController::class, 'menuTree']);

        Route::get('admin-users', [MetaController::class, 'adminUsers']);
        Route::post('admin-users', [AdminUserController::class, 'create']);
        Route::put('admin-users/:id/reset-password', [AdminUserController::class, 'resetPassword']);
        Route::put('admin-users/:id', [AdminUserController::class, 'update']);
        Route::delete('admin-users/:id', [AdminUserController::class, 'delete']);

        Route::get('roles', [MetaController::class, 'roles']);
        Route::post('roles', [RoleController::class, 'create']);
        Route::put('roles/:id', [RoleController::class, 'update']);
        Route::delete('roles/:id', [RoleController::class, 'delete']);

        Route::get('departments/tree', [MetaController::class, 'departments']);
        Route::post('departments', [DepartmentController::class, 'create']);
        Route::put('departments/:id', [DepartmentController::class, 'update']);
        Route::delete('departments/:id', [DepartmentController::class, 'delete']);

        Route::get('permissions/tree', [MetaController::class, 'permissions']);
        Route::post('permissions', [PermissionController::class, 'create']);
        Route::put('permissions/:id', [PermissionController::class, 'update']);
        Route::delete('permissions/:id', [PermissionController::class, 'delete']);

        Route::get('config/items', [MetaController::class, 'configItems']);
        Route::post('config/items', [ConfigItemController::class, 'create']);
        Route::put('config/items/batch-save', [ConfigItemController::class, 'batchSave']);
        Route::put('config/items/:id', [ConfigItemController::class, 'update']);
        Route::delete('config/items/:id', [ConfigItemController::class, 'delete']);

        Route::get('login-logs', [MetaController::class, 'loginLogs']);
        Route::get('operation-logs', [MetaController::class, 'operationLogs']);

        Route::get('attachments/config', [AttachmentController::class, 'config']);
        Route::get('attachments', [AttachmentController::class, 'index']);
        Route::post('attachments/upload', [AttachmentController::class, 'upload']);
        Route::post('attachments/credentials', [AttachmentController::class, 'credentials']);
        Route::post('attachments/record', [AttachmentController::class, 'record']);
        Route::delete('attachments/:id', [AttachmentController::class, 'delete']);
    })->middleware('admin.auth');
});
