import axios from 'axios'

const API_BASE = '/api'

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken })
          localStorage.setItem('accessToken', response.data.accessToken)
          localStorage.setItem('refreshToken', response.data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`
          return api(originalRequest)
        }
      } catch {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  sendOtp: (phoneNumber: string) => api.post('/auth/send-otp', { phoneNumber }),
  verifyOtp: (phoneNumber: string, otp: string) => api.post('/auth/verify-otp', { phoneNumber, otp }),
  refreshToken: (refreshToken: string) => api.post('/auth/refresh', { refreshToken }),
  getProfile: () => api.get('/auth/me'),
  updateOptIn: (optIn: boolean) => api.put('/auth/me/opt-in', { optIn }),
  updateTimezone: (timezone: string) => api.put('/auth/me/timezone', { timezone }),
}

export const competitionApi = {
  getAll: () => api.get('/competitions'),
  getById: (id: string) => api.get(`/competitions/${id}`),
  getMatches: (id: string, params?: { status?: string; date?: string }) => 
    api.get(`/competitions/${id}/matches`, { params }),
}

export const matchApi = {
  getLive: () => api.get('/matches/live'),
  getUpcoming: (hours = 24) => api.get('/matches/upcoming', { params: { hours } }),
  getFinished: (days = 7) => api.get('/matches/finished', { params: { days } }),
  getDetail: (id: string) => api.get(`/matches/${id}`),
  getEvents: (id: string) => api.get(`/matches/${id}/events`),
  favorite: (id: string) => api.post(`/matches/${id}/favorite`),
  unfavorite: (id: string) => api.delete(`/matches/${id}/favorite`),
}

export const favoriteApi = {
  getMatches: () => api.get('/favorites/matches'),
  getTeams: () => api.get('/favorites/teams'),
  favoriteTeam: (teamId: string) => api.post(`/favorites/teams/${teamId}`),
  unfavoriteTeam: (teamId: string) => api.delete(`/favorites/teams/${teamId}`),
}

export const notificationApi = {
  getAll: () => api.get('/notifications'),
  getPreferences: () => api.get('/notifications/preferences'),
  updatePreferences: (preferences: any) => api.put('/notifications/preferences', preferences),
}

export default api