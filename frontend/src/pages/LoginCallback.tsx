import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import Loading from '../components/Loading'

function LoginCallback() {
  const auth = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (auth.isAuthenticated) {
      navigate('/', { replace: true })
    }
  }, [auth.isAuthenticated, navigate])

  if (auth.error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-lg shadow-md">
          <h1 className="text-xl font-bold text-red-600 mb-4">Login Failed</h1>
          <p className="text-gray-600">{auth.error.message}</p>
          <button
            onClick={() => auth.signinRedirect()}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Try Again
          </button>
        </div>
      </div>
    )
  }

  return <Loading message="Completing login..." />
}

export default LoginCallback
