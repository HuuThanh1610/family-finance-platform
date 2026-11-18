import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/layout/AppShell'
import { ForgotPasswordPage } from '@/features/auth/ForgotPasswordPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { OidcCallbackPage } from '@/features/auth/OidcCallbackPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { useAuth } from '@/features/auth/AuthContext'
import { AllowancesPage } from '@/features/allowance/AllowancesPage'
import { AnalyticsPage } from '@/features/analytics/AnalyticsPage'
import { BudgetsPage } from '@/features/budget/BudgetsPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { ExpensesPage } from '@/features/expense/ExpensesPage'
import { FamilyPage } from '@/features/family/FamilyPage'
import { HouseholdPage } from '@/features/household/HouseholdPage'
import { NotificationPreferencesPage } from '@/features/notification/NotificationPreferencesPage'
import { NotificationsPage } from '@/features/notification/NotificationsPage'
import { PaymentsPage } from '@/features/payment/PaymentsPage'
import { WalletsPage } from '@/features/wallet/WalletsPage'

function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth/callback" element={<OidcCallbackPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/family" element={<FamilyPage />} />
          <Route path="/wallets" element={<WalletsPage />} />
          <Route path="/payments" element={<PaymentsPage />} />
          <Route path="/budgets" element={<BudgetsPage />} />
          <Route path="/allowances" element={<AllowancesPage />} />
          <Route path="/expenses" element={<ExpensesPage />} />
          <Route path="/household" element={<HouseholdPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/notifications/preferences" element={<NotificationPreferencesPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
