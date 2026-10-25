import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navigation = [
  { name: 'Home', href: '/' },
  { name: 'Favorites', href: '/favorites' },
  { name: 'Settings', href: '/settings' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const location = useLocation()

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="Main navigation">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center gap-8">
              <Link to="/" className="text-xl font-bold text-primary-600">
                ⚽ Football Score
              </Link>
              <div className="hidden md:flex md:gap-6">
                {navigation.map((item) => (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      location.pathname === item.href || 
                      (item.href !== '/' && location.pathname.startsWith(item.href))
                        ? 'bg-primary-50 text-primary-700'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    {item.name}
                  </Link>
                ))}
              </div>
            </div>
            <div className="flex items-center gap-4">
              {user && (
                <span className="text-sm text-gray-500 hidden sm:block">
                  {user.phoneNumber}
                </span>
              )}
              <button
                onClick={logout}
                className="btn-secondary text-sm"
              >
                Logout
              </button>
            </div>
          </div>
        </nav>
      </header>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <Outlet />
      </main>
    </div>
  )
}