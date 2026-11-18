import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth/AuthContext'
import { FamilyProvider } from '@/features/family/FamilyContext'
import { AppRoutes } from '@/routes'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <FamilyProvider>
          <AppRoutes />
        </FamilyProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
