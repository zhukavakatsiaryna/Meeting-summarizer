import { useEffect, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { meetingsApi, MeetingResponse, SummaryResponse } from '../api/client'
import { Navbar } from '../components/Navbar'
import { StatusBadge } from '../components/StatusBadge'

const POLL_INTERVAL_MS = 5000

export function MeetingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [meeting, setMeeting] = useState<MeetingResponse | null>(null)
  const [summary, setSummary] = useState<SummaryResponse | null>(null)
  const [error, setError] = useState('')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const fetchMeeting = async () => {
    if (!id) return
    try {
      const { data } = await meetingsApi.get(id)
      setMeeting(data)
      if (data.status === 'COMPLETED') {
        const { data: sum } = await meetingsApi.getSummary(id)
        setSummary(sum)
        stopPolling()
      } else if (data.status === 'FAILED') {
        stopPolling()
      }
    } catch {
      setError('Failed to load meeting.')
      stopPolling()
    }
  }

  const stopPolling = () => {
    if (pollRef.current) clearInterval(pollRef.current)
  }

  useEffect(() => {
    fetchMeeting()
    pollRef.current = setInterval(fetchMeeting, POLL_INTERVAL_MS)
    return stopPolling
  }, [id])

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="max-w-3xl mx-auto px-4 py-8">
        <Link to="/meetings" className="text-sm text-indigo-600 hover:underline mb-4 inline-block">
          ← Back to meetings
        </Link>

        {error && <p className="text-sm text-red-600">{error}</p>}

        {meeting && (
          <div className="space-y-6">
            <div className="bg-white border border-gray-200 rounded-xl px-5 py-4 flex items-center justify-between">
              <div>
                <h1 className="text-lg font-semibold text-gray-900">{meeting.title}</h1>
                <p className="text-xs text-gray-400 mt-0.5">
                  {new Date(meeting.createdAt).toLocaleString()}
                </p>
              </div>
              <StatusBadge status={meeting.status} />
            </div>

            {(meeting.status === 'UPLOADED' || meeting.status === 'PROCESSING') && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-xl px-5 py-4 text-sm text-yellow-700">
                Processing your meeting… this page will update automatically.
              </div>
            )}

            {meeting.status === 'FAILED' && (
              <div className="bg-red-50 border border-red-200 rounded-xl px-5 py-4 text-sm text-red-700">
                Processing failed for this meeting.
              </div>
            )}

            {summary && (
              <div className="space-y-4">
                <Section title="Summary">
                  <p className="text-sm text-gray-700">{summary.shortSummary}</p>
                </Section>

                <Section title="Detailed summary">
                  <p className="text-sm text-gray-700 whitespace-pre-line">{summary.detailedSummary}</p>
                </Section>

                {summary.actionItems.length > 0 && (
                  <Section title="Action items">
                    <BulletList items={summary.actionItems} />
                  </Section>
                )}

                {summary.decisions.length > 0 && (
                  <Section title="Decisions">
                    <BulletList items={summary.decisions} />
                  </Section>
                )}

                {summary.blockers.length > 0 && (
                  <Section title="Blockers">
                    <BulletList items={summary.blockers} />
                  </Section>
                )}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl px-5 py-4">
      <h2 className="text-sm font-semibold text-gray-900 mb-2">{title}</h2>
      {children}
    </div>
  )
}

function BulletList({ items }: { items: string[] }) {
  return (
    <ul className="list-disc list-inside space-y-1">
      {items.map((item, i) => (
        <li key={i} className="text-sm text-gray-700">{item}</li>
      ))}
    </ul>
  )
}
