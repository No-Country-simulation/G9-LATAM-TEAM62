import { Navigate, Outlet } from 'react-router-dom'
import { AppHeader } from '../components/layout/AppHeader'
import { useAuth } from '../context/useAuth'

function RequireAuth() {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return (
    <>
      <AppHeader />
      <Outlet />
    </>
  )
}

export default RequireAuth
