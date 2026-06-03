import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export interface MeetingResponse {
  id: string
  title: string
  status: 'UPLOADED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}

export interface SummaryResponse {
  id: string
  meetingId: string
  shortSummary: string
  detailedSummary: string
  actionItems: string[]
  decisions: string[]
  blockers: string[]
  createdAt: string
}

export const authApi = {
  register: (email: string, password: string) =>
    api.post('/auth/register', { email, password }),
  login: (email: string, password: string) =>
    api.post<{ token: string; email: string; role: string }>('/auth/login', { email, password }),
}

export const meetingsApi = {
  list: () => api.get<MeetingResponse[]>('/meetings'),
  get: (id: string) => api.get<MeetingResponse>(`/meetings/${id}`),
  getSummary: (id: string) => api.get<SummaryResponse>(`/meetings/${id}/summary`),
  upload: (title: string, file: File) => {
    const form = new FormData()
    form.append('title', title)
    form.append('file', file)
    return api.post<MeetingResponse>('/meetings/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
