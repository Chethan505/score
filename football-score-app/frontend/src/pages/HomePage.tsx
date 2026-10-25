import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { competitionApi } from '../services/api'
import { Competition } from '../types'

export default function HomePage() {
  const { data: competitions, isLoading, error } = useQuery({
    queryKey: ['competitions'],
    queryFn: () => competitionApi.getAll(),
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">Failed to load competitions</p>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Competitions</h1>
        <p className="text-gray-500 mt-1">Select a competition to view matches</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {competitions?.map((comp: Competition) => (
          <Link
            key={comp.id}
            to={`/competition/${comp.id}`}
            className="card hover:shadow-md transition-shadow"
          >
            <div className="p-5">
              <div className="flex items-center gap-4">
                {comp.logoUrl && (
                  <img
                    src={comp.logoUrl}
                    alt={comp.name}
                    className="w-12 h-12 rounded-lg object-cover"
                  />
                )}
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-900 truncate">{comp.name}</h3>
                  <p className="text-sm text-gray-500">{comp.country}</p>
                </div>
              </div>
            </div>
          </Link>
        ))}
      </div>

      {competitions?.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">No competitions available</p>
        </div>
      )}
    </div>
  )
}