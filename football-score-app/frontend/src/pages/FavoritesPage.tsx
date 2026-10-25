import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { favoriteApi } from '../services/api'
import { MatchSummary, Team } from '../types'
import { format, formatDistanceToNow } from 'date-fns'
import { useAuth } from '../context/AuthContext'

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

function TeamCard({ team }: { team: Team }) {
  return (
    <div className="card p-4 flex items-center gap-4">
      {team.logoUrl && (
        <img src={team.logoUrl} alt={team.name} className="w-12 h-12 rounded-lg object-cover" />
      )}
      <div>
        <h3 className="font-semibold text-gray-900">{team.name}</h3>
        <p className="text-sm text-gray-500">{team.shortName}</p>
      </div>
    </div>
  )
}

export default function FavoritesPage() {
  const { user, loading: authLoading } = useAuth()

  const { data: matches, isLoading: matchesLoading } = useQuery({
    queryKey: ['favoriteMatches'],
    queryFn: () => favoriteApi.getMatches(),
    enabled: !!user,
  })

  const { data: teams, isLoading: teamsLoading } = useQuery({
    queryKey: ['favoriteTeams'],
    queryFn: () => favoriteApi.getTeams(),
    enabled: !!user,
  })

  if (authLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Please log in to view favorites</p>
      </div>
    )
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Favorites</h1>

      {(matches && matches.length > 0) && (
        <section className="mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Favorite Matches ({matches.length})</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {matches.map((match) => <MatchCard key={match.id} match={match} />)}
          </div>
        </section>
      )}

      {(teams && teams.length > 0) && (
        <section>
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Favorite Teams ({teams.length})</h2>
          <div className="space-y-3">
            {teams.map((team) => <TeamCard key={team.id} team={team} />)}
          </div>
        </section>
      )}

      {(!matches || matches.length === 0) && (!teams || teams.length === 0) && (
        <div className="text-center py-12">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <h3 className="mt-2 text-lg font-medium text-gray-900">No favorites yet</h3>
          <p className="mt-1 text-gray-500">Star matches or teams to see them here</p>
        </div>
      )}
    </div>
  )
}