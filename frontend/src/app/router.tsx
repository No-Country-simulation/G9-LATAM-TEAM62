import { createBrowserRouter } from 'react-router-dom'
import AnalysisLayout from '../pages/analysis/AnalysisLayout'
import ProcessingPage from '../pages/analysis/ProcessingPage'
import ProfilePage from '../pages/analysis/ProfilePage'
import ResultsPage from '../pages/analysis/ResultsPage'
import ReviewPage from '../pages/analysis/ReviewPage'
import TransactionsPage from '../pages/analysis/TransactionsPage'
import AuthLayout from '../pages/auth/AuthLayout'
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import DashboardPage from '../pages/DashboardPage'
import LandingPage from '../pages/LandingPage'
import RequireAuth from './RequireAuth'

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      { path: '/dashboard', element: <DashboardPage /> },
      {
        element: <AnalysisLayout />,
        children: [
          { path: '/analysis/new', element: <ProfilePage /> },
          { path: '/analysis/transactions', element: <TransactionsPage /> },
          { path: '/analysis/review', element: <ReviewPage /> },
        ],
      },
      { path: '/analysis/processing', element: <ProcessingPage /> },
      { path: '/analysis/results', element: <ResultsPage /> },
    ],
  },
])
