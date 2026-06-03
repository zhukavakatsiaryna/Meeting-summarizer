import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export function Navbar() {
  const { email, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <Link to="/meetings" className="text-lg font-semibold text-gray-900">
        Meeting Summarizer
      </Link>
      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-500">{email}</span>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-600 hover:text-gray-900"
        >
          Sign out
        </button>
      </div>
    </nav>
  )
}
