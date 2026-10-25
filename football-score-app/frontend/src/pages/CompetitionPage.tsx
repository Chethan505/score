import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { competitionApi, matchApi } from '../services/api'
import { MatchSummary } from '../types'
import { format, formatDistanceToNow } from 'date-fns'

const statusConfig = {
  LIVE: { label: 'LIVE', className: 'badge-live' },
  HALFTIME: { label: 'HT', className: 'badge-live' },
  EXTRA_TIME: { label: 'ET', className: 'badge-live' },
  PENALTIES: { label: 'PEN', className: 'badge-live' },
  FINISHED: { label: 'FT', className: 'badge-finished' },
  POSTPONED: { label: 'POSTP.', className: 'badge-finished' },
  CANCELLED: { label: 'CANC.', className: 'badge-finished' },
  SCHEDULED: { label: 'UPCOMING', className: 'badge-upcoming' },
}

function MatchCard({ match }: { match: MatchSummary }) {
  const status = statusConfig[match.status as keyof typeof statusConfig] || { label: match.status, className: 'badge-upcoming' }
  const isLive = ['LIVE', 'HALFTIME', 'EXTRA_TIME', 'PENALTIES'].includes(match.status)
  
  return (
    <Link to={`/match/${match.id}`} className="card hover:shadow-md transition-shadow">
      <div className="p-4">
        <div className="flex items-center justify-between mb-3">
          <span className={status.className}>{status.label}</span>
          {isLive && match.minute && (
            <span className="text-sm font-medium text-red-600">{match.minute}'</span>
          )}
        </div>
        
        <div className="flex items-center justify-between">
          <div className="flex flex-col items-center flex-1 text-right pr-3 min-w-0">
            {match.homeTeamLogoUrl && (
              <img src={match.homeTeamLogoUrl} alt={match.homeTeamName} className="w-10 h-10 rounded-lg object-cover mb-1" />
            )}
            <span className="text-sm font-medium truncate block w-full">{match.homeTeamShortName || match.homeTeamName}</span>
          </div>
          
          <div className="flex flex-col items-center flex-1 px-4">
            {isLive ? (
              <>
                <span className="text-2xl font-bold text-gray-900">{match.homeScore} - {match.awayScore}</span>
                <span className="text-xs text-gray-500">{match.extraMinute ? `${match.minute}+${match.extraMinute}'` : ''}</span>
              </>
            ) : (
              <>
                <span className="text-lg font-medium text-gray-900">
                  {format(new Date(match.scheduledStart), 'MMM d, HH:mm')}
                </span>
                <span className="text-xs text-gray-500">
                  {formatDistanceToNow(new Date(match.scheduledStart), { addSuffix: true })}
                </span>
              </>
            )}
          </div>
          
          <div className="flex flex-col items-center flex-1 text-left pl-3 min-w-0">
            {match.awayTeamLogoUrl && (
              <img src={match.awayTeamLogoUrl} alt={match.awayTeamName} className="w-10 h-10 rounded-lg object-cover mb-1" />
            )}
            <span className="text-sm font-medium truncate block w-full">{match.awayTeamShortName || match.awayTeamName}</span>
          </div>
        </div>
      </div>
    </Link>
  )
}

export default function CompetitionPage() {
  const { id } = useParams<{ id: string }>()
  const [statusFilter, setStatusFilter] = useState<string>('')

  const { data: matches, isLoading } = useQuery({
    queryKey: ['matches', id, statusFilter],
    queryFn: () => competitionApi.getMatches(id!, statusFilter || undefined),
    enabled: !!id,
  })

  const { data: competition } = useQuery({
    queryKey: ['competition', id],
    queryFn: () => competitionApi.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
      </div>
    )
  }

  const filteredMatches = matches || []
  const liveMatches = filteredMatches.filter(m => ['LIVE', 'HALFTIME', 'EXTRA_TIME', 'PENALTIES'].includes(m.status))
  const upcomingMatches = filteredMatches.filter(m => m.status === 'SCHEDULED')
  const finishedMatches = filteredMatches.filter(m => ['FINISHED', 'POSTPONED', 'CANCELLED'].includes(m.status))

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <Link to="/" className="text-primary-600 hover:underline text-sm mb-2 inline-block">← Back</Link>
          <h1 className="text-2xl font-bold text-gray-900">{competition?.name}</h1>
        </div>
        <div className="flex gap-2">
          {['', 'LIVE', 'SCHEDULED', 'FINISHED'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3 py-1 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === status
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {status || 'All'}
            </button>
          ))}
        </div>
      </div>

      {liveMatches.length > 0 && (
        <section className="mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <span className="badge-live">LIVE</span>
            Live Matches ({liveMatches.length})
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {liveMatches.map((match) => <MatchCard key={match.id} match={match} />)}
          </div>
        </section>
      )}

      {upcomingMatches.length > 0 && (
        <section className="mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <span className="badge-upcoming">UPCOMING</span>
            Upcoming ({upcomingMatches.length})
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {upcomingMatches.map((match) => <MatchCard key={match.id} match={match} />)}
          </div>
        </section>
      )}

      {finishedMatches.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <span className="badge-finished">FINISHED</span>
            Finished ({finishedMatches.length})
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {finishedMatches.map((match) => <MatchCard key={match.id} match={match} />)}
          </div>
        </section>
      )}

      {filteredMatches.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">No matches found</p>
        </div>
      )}
    </div>
  )
}