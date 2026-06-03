import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { meetingsApi, MeetingResponse } from '../api/client'
import { Navbar } from '../components/Navbar'
import { StatusBadge } from '../components/StatusBadge'
import { UploadModal } from '../components/UploadModal'

export function MeetingsPage() {
  const navigate = useNavigate()
  const [meetings, setMeetings] = useState<MeetingResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [showUpload, setShowUpload] = useState(false)

  useEffect(() => {
    meetingsApi.list().then(({ data }) => setMeetings(data)).finally(() => setLoading(false))
  }, [])

  const handleUploaded = (meeting: MeetingResponse) => {
    setMeetings((prev) => [meeting, ...prev])
    setShowUpload(false)
    navigate(`/meetings/${meeting.id}`)
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="max-w-3xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-gray-900">Your meetings</h1>
          <button
            onClick={() => setShowUpload(true)}
            className="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700"
          >
            Upload meeting
          </button>
        </div>

        {loading && <p className="text-sm text-gray-500">Loading…</p>}

        {!loading && meetings.length === 0 && (
          <div className="text-center py-16 text-gray-400">
            <p className="text-sm">No meetings yet. Upload one to get started.</p>
          </div>
        )}

        <ul className="space-y-3">
          {meetings.map((m) => (
            <li
              key={m.id}
              onClick={() => navigate(`/meetings/${m.id}`)}
              className="bg-white border border-gray-200 rounded-xl px-5 py-4 flex items-center justify-between cursor-pointer hover:border-indigo-300 transition-colors"
            >
              <div>
                <p className="font-medium text-gray-900">{m.title}</p>
                <p className="text-xs text-gray-400 mt-0.5">
                  {new Date(m.createdAt).toLocaleString()}
                </p>
              </div>
              <StatusBadge status={m.status} />
            </li>
          ))}
        </ul>
      </main>

      {showUpload && (
        <UploadModal onClose={() => setShowUpload(false)} onUploaded={handleUploaded} />
      )}
    </div>
  )
}
