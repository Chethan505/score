import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, verifyOtp, loading: authLoading } = useAuth()
  const [step, setStep] = useState<'phone' | 'otp'>('phone')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handlePhoneSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(phoneNumber)
      setStep('otp')
    } catch (err) {
      setError('Failed to send OTP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await verifyOtp(phoneNumber, otp)
      navigate('/')
    } catch (err) {
      setError('Invalid OTP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const formatPhone = (value: string) => {
    const digits = value.replace(/\D/g, '')
    if (digits.startsWith('91') && digits.length > 2) {
      return `+${digits}`
    }
    if (digits.length > 0) {
      return `+${digits}`
    }
    return ''
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md">
        <div className="card p-8">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900">Football Score</h1>
            <p className="text-gray-500 mt-2">Sign in to get match notifications</p>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}

          {step === 'phone' && (
            <form onSubmit={handlePhoneSubmit} className="space-y-6">
              <div>
                <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
                  Phone Number
                </label>
                <input
                  id="phone"
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(formatPhone(e.target.value))}
                  placeholder="+91 98765 43210"
                  className="input"
                  required
                  disabled={loading}
                  autoComplete="tel"
                />
              </div>
              <button type="submit" disabled={loading || phoneNumber.length < 10} className="btn-primary w-full">
                {loading ? 'Sending...' : 'Send OTP via WhatsApp'}
              </button>
            </form>
          )}

          {step === 'otp' && (
            <form onSubmit={handleOtpSubmit} className="space-y-6">
              <div>
                <label htmlFor="otp" className="block text-sm font-medium text-gray-700 mb-1">
                  Enter OTP sent to {phoneNumber}
                </label>
                <input
                  id="otp"
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                  placeholder="123456"
                  maxLength={6}
                  className="input text-center text-2xl tracking-widest"
                  required
                  disabled={loading}
                  autoComplete="one-time-code"
                />
              </div>
              <button type="submit" disabled={loading || otp.length !== 6} className="btn-primary w-full">
                {loading ? 'Verifying...' : 'Verify & Sign In'}
              </button>
              <button
                type="button"
                onClick={() => setStep('phone')}
                disabled={loading}
                className="btn-secondary w-full"
              >
                Back
              </button>
            </form>
          )}

          <div className="mt-8 text-center text-sm text-gray-500">
            <p>By signing in, you agree to receive WhatsApp notifications for your favorite matches.</p>
          </div>
        </div>
      </div>
    </div>
  )
}