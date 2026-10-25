import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { matchApi, favoriteApi } from '../services/api'
import { MatchDetail, MatchEvent } from '../types'
import { format, formatDistanceToNow } from 'date-fns'
import { useAuth } from '../context/AuthContext'
import { useState } from 'react'

const eventIcons: Record<string, string> = {
  GOAL: '⚽',
  YELLOW_CARD: '🟨',
  RED_CARD: '🟥',
  SUBSTITUTION: '🔄',
  HALFTIME: '🍊',
  FULLTIME: '🏁',
  EXTRA_TIME_START: '⏱️',
  PENALTY_SHOOTOUT: '⚽',
  MATCH_START: '⚽',
  POSTPONED: '⏸️',
  CANCELLED: '❌',
}

const eventColors: Record<string, string> = {
  GOAL: 'text-green-600 bg-green-50',
  YELLOW_CARD: 'text-yellow-700 bg-yellow-50',
  RED_CARD: 'text-red-600 bg-red-50',
  SUBSTITUTION: 'text-blue-600 bg-blue-50',
  HALFTIME: 'text-orange-600 bg-orange-50',
  FULLTIME: 'text-gray-600 bg-gray-50',
  DEFAULT: 'text-gray-600 bg-gray-50',
}

function EventTimeline({ events }: { events: MatchEvent[] }) {
  if (events.length === 0) {
    return <p className="text-center text-gray-500 py-8">No events yet</p>
  }

  return (
    <div className="space-y-3">
      {events.map((event) => (
        <div key={event.id} className="flex gap-3">
          <div className="flex-shrink-0 w-10 text-center text-sm text-gray-500">
            {event.minute !== null ? `${event.minute}'` : ''}
            {event.extraMinute && event.extraMinute > 0 && `+${event.extraMinute}`}
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <span className={`px-2 py-0.5 rounded text-xs font-medium ${eventColors[event.eventType] || eventColors.DEFAULT}`}>
                {eventIcons[event.eventType] || '📢'} {event.eventType.replace('_', ' ')}
              </span>
              {event.team && (
                <span className="text-sm font-medium text-gray-700">{event.team.name}</span>
              )}
            </div>
            <p className="text-sm text-gray-600 mt-1">
              {event.playerName ? `${event.playerName} ` : ''}
              {event.description || ''}
            </p>
          </div>
        </div>
      ))}
    </div>
  )
}

export default function MatchDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const [isFavorited, setIsFavorited] = useState(false)
  const [favoriteLoading, setFavoriteLoading] = useState(false)

  const { data: match, isLoading, error } = useQuery({
    queryKey: ['match', id],
    queryFn: () => matchApi.getDetail(id!),
    enabled: !!id,
  })

  const { data: events } = useQuery({
    queryKey: ['matchEvents', id],
    queryFn: () => matchApi.getEvents(id!),
    enabled: !!id,
  })

  const checkFavorite = async () => {
    if (!user) return
    try {
      // This would need a new API endpoint
      // For now, we'll skip
    } catch {}
  }

  const handleFavorite = async () => {
    if (!user || favoriteLoading) return
    setFavoriteLoading(true)
    try {
      if (isFavorited) {
        await matchApi.unfavorite(id!)
        setIsFavorited(false)
      } else {
        await matchApi.favorite(id!)
        setIsFavorited(true)
      }
    } catch (error) {
      console.error('Failed to toggle favorite', error)
    } finally {
      setFavoriteLoading(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
      </div>
    )
  }

  if (error || !match) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">Match not found</p>
      </div>
    )
  }

  const statusConfig: Record<string, { label: string; className: string }> = {
    LIVE: { label: 'LIVE', className: 'badge-live' },
    HALFTIME: { label: 'HT', className: 'badge-live' },
    EXTRA_TIME: { label: 'ET', className: 'badge-live' },
    PENALTIES: { label: 'PEN', className: 'badge-live' },
    FINISHED: { label: 'FT', className: 'badge-finished' },
    POSTPONED: { label: 'POSTPONED', className: 'badge-finished' },
    CANCELLED: { label: 'CANCELLED', className: 'badge-finished' },
    SCHEDULED: { label: 'UPCOMING', className: 'badge-upcoming' },
  }

  const status = statusConfig[match.status] || { label: match.status, className: 'badge-upcoming' }
  const isLive = ['LIVE', 'HALFTIME', 'EXTRA_TIME', 'PENALTIES'].includes(match.status)

  return (
    <div className="max-w-3xl mx-auto">
      <div className="card mb-6">
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <span className={status.className}>{status.label}</span>
            <button
              onClick={handleFavorite}
              disabled={favoriteLoading || !user}
              className={`p-2 rounded-lg transition-colors ${
                isFavorited ? 'bg-yellow-100 text-yellow-600' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              } disabled:opacity-50`}
              aria-label={isFavorited ? 'Remove from favorites' : 'Add to favorites'}
            >
              <svg className="w-5 h-5" fill={isFavorited ? 'currentColor' : 'none'} stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
              </svg>
            </button>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div className="flex flex-col items-center flex-1 text-right pr-4 min-w-0">
              {match.homeTeam.logoUrl && (
                <img src={match.homeTeam.logoUrl} alt={match.homeTeam.name} className="w-16 h-16 rounded-lg object-cover mb-2" />
              )}
              <h2 className="text-lg font-bold text-gray-900 truncate w-full">{match.homeTeam.name}</h2>
            </div>

            <div className="flex flex-col items-center flex-1 px-6">
              {isLive ? (
                <>
                  <p className="text-4xl font-bold text-gray-900">{match.homeScore} - {match.awayScore}</p>
                  <p className="text-sm text-gray-500">
                    {match.minute !== null ? `${match.minute}'` : ''}
                    {match.extraMinute && match.extraMinute > 0 && `+${match.extraMinute}`}
                  </p>
                </>
              ) : (
                <>
                  <p className="text-lg font-medium text-gray-900">
                    {format(new Date(match.scheduledStart), 'EEEE, MMMM d, yyyy HH:mm')}
                  </p>
                  <p className="text-sm text-gray-500">
                    {formatDistanceToNow(new Date(match.scheduledStart), { addSuffix: true })}
                  </p>
                </>
              )}
            </div>

            <div className="flex flex-col items-center flex-1 text-left pl-4 min-w-0">
              {match.awayTeam.logoUrl && (
                <img src={match.awayTeam.logoUrl} alt={match.awayTeam.name} className="w-16 h-16 rounded-lg object-cover mb-2" />
              )}
              <h2 className="text-lg font-bold text-gray-900 truncate w-full">{match.awayTeam.name}</h2>
            </div>
          </div>

          <div className="mt-4 text-center text-sm text-gray-500">
            {match.competitionName}
          </div>
        </div>
      </div>

      <div className="card">
        <div className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Timeline</h3>
          <EventTimeline events={events || []} />
        </div>
      </div>
    </div>
  )
}