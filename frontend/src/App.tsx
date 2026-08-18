import { RouterProvider } from 'react-router-dom'
import { router } from './app/router'
import { AnalysisProvider } from './context/AnalysisContext'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastProvider'

function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <AnalysisProvider>
          <RouterProvider router={router} />
        </AnalysisProvider>
      </AuthProvider>
    </ToastProvider>
  )
}

export default App
