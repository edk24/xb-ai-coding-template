import { Routes, Route, Navigate } from 'react-router-dom'
import AuthGuard from '../components/AuthGuard'
import PermissionRoute from '../components/PermissionRoute'
import AdminLayout from '../layouts/AdminLayout'
import Login from '../pages/login'
import Dashboard from '../pages/dashboard'
import Profile from '../pages/profile'
import Password from '../pages/password'
import AdminUsers from '../pages/system/admin-users'
import Roles from '../pages/system/roles'
import Departments from '../pages/system/departments'
import Permissions from '../pages/system/permissions'
import Config from '../pages/system/config'
import ScheduledJobs from '../pages/system/scheduled-jobs'
import LoginLogs from '../pages/logs/login'
import OperationLogs from '../pages/logs/operation'
import Attachments from '../pages/attachments'

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
        <Route path="dashboard" element={<PermissionRoute><Dashboard /></PermissionRoute>} />
        <Route path="profile/basic" element={<Profile />} />
        <Route path="profile/password" element={<Password />} />
        <Route path="system/admin-users" element={<PermissionRoute><AdminUsers /></PermissionRoute>} />
        <Route path="system/roles" element={<PermissionRoute><Roles /></PermissionRoute>} />
        <Route path="system/departments" element={<PermissionRoute><Departments /></PermissionRoute>} />
        <Route path="system/permissions" element={<PermissionRoute><Permissions /></PermissionRoute>} />
        <Route path="system/config" element={<PermissionRoute><Config /></PermissionRoute>} />
        <Route path="system/scheduled-jobs" element={<PermissionRoute><ScheduledJobs /></PermissionRoute>} />
        <Route path="logs/login" element={<PermissionRoute><LoginLogs /></PermissionRoute>} />
        <Route path="logs/operation" element={<PermissionRoute><OperationLogs /></PermissionRoute>} />
        <Route path="attachments" element={<PermissionRoute><Attachments /></PermissionRoute>} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
