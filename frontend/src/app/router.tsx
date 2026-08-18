import { createBrowserRouter } from 'react-router-dom'
import AccountPage from '../pages/account/AccountPage'
import AnalysisLayout from '../pages/analysis/AnalysisLayout'
import ProcessingPage from '../pages/analysis/ProcessingPage'
import ResultsPage from '../pages/analysis/ResultsPage'
import ReviewPage from '../pages/analysis/ReviewPage'
import TransactionsPage from '../pages/analysis/TransactionsPage'
import AuthLayout from '../pages/auth/AuthLayout'
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import DashboardPage from '../pages/DashboardPage'
import LandingPage from '../pages/LandingPage'
import RecommendationsHistoryPage from '../pages/recommendations/RecommendationsHistoryPage'
import TransactionsHistoryPage from '../pages/transactions/TransactionsHistoryPage'
import UploadStatementPage from '../pages/transactions/UploadStatementPage'
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
      { path: '/profile', element: <AccountPage /> },
      { path: '/transactions', element: <TransactionsHistoryPage /> },
      { path: '/transactions/upload', element: <UploadStatementPage /> },
      { path: '/recommendations', element: <RecommendationsHistoryPage /> },
      {
        element: <AnalysisLayout />,
        children: [
          { path: '/analysis/transactions', element: <TransactionsPage /> },
          { path: '/analysis/review', element: <ReviewPage /> },
        ],
      },
      { path: '/analysis/processing', element: <ProcessingPage /> },
      { path: '/analysis/results', element: <ResultsPage /> },
    ],
  },
])
