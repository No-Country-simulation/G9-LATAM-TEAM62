import { RouterProvider } from 'react-router-dom'
import { router } from './app/router'
import { AnalysisProvider } from './context/AnalysisContext'
import { AuthProvider } from './context/AuthContext'

function App() {
  return (
    <AuthProvider>
      <AnalysisProvider>
        <RouterProvider router={router} />
      </AnalysisProvider>
    </AuthProvider>
  )
}

export default App
