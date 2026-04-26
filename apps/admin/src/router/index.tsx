import { Routes, Route, Navigate } from 'react-router-dom'
import AuthGuard from '../components/AuthGuard'
import AdminLayout from '../layouts/AdminLayout'
import Login from '../pages/login'
import Dashboard from '../pages/dashboard'
import Profile from '../pages/profile'
import Password from '../pages/password'
import AdminUsers from '../pages/system/admin-users'
import Roles from '../pages/system/roles'
import Departments from '../pages/system/departments'
import Permissions from '../pages/system/permissions'
import LoginLogs from '../pages/logs/login'
import OperationLogs from '../pages/logs/operation'

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <AuthGuard>
            <AdminLayout />
          </AuthGuard>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="profile/basic" element={<Profile />} />
        <Route path="profile/password" element={<Password />} />
        <Route path="system/admin-users" element={<AdminUsers />} />
        <Route path="system/roles" element={<Roles />} />
        <Route path="system/departments" element={<Departments />} />
        <Route path="system/permissions" element={<Permissions />} />
        <Route path="logs/login" element={<LoginLogs />} />
        <Route path="logs/operation" element={<OperationLogs />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
