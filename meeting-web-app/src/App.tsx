import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { MeetingsPage } from './pages/MeetingsPage'
import { MeetingDetailPage } from './pages/MeetingDetailPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/meetings"
            element={<ProtectedRoute><MeetingsPage /></ProtectedRoute>}
          />
          <Route
            path="/meetings/:id"
            element={<ProtectedRoute><MeetingDetailPage /></ProtectedRoute>}
          />
          <Route path="*" element={<Navigate to="/meetings" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
