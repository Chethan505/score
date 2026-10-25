import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { notificationApi } from '../services/api'
import { NotificationPreferences } from '../types'

export default function SettingsPage() {
  const { user, updateOptIn, updateTimezone } = useAuth()
  const queryClient = useQueryClient()

  const { data: preferences, isLoading } = useQuery({
    queryKey: ['notificationPreferences'],
    queryFn: () => notificationApi.getPreferences(),
    enabled: !!user,
  })

  const [formData, setFormData] = useState<NotificationPreferences>({
    matchStartReminder: true,
    goalNotifications: true,
    cardNotifications: false,
    substitutionNotifications: false,
    halfTimeNotifications: false,
    fullTimeNotifications: true,
    reminderMinutesBefore: 15,
    quietHoursStart: null,
    quietHoursEnd: null,
  })

  const [optIn, setOptIn] = useState(false)
  const [timezone, setTimezone] = useState('Asia/Kolkata')

  const updatePreferencesMutation = useMutation({
    mutationFn: (data: NotificationPreferences) => notificationApi.updatePreferences(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationPreferences'] })
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    updatePreferencesMutation.mutate(formData)
  }

  const handleOptInChange = async (checked: boolean) => {
    setOptIn(checked)
    await updateOptIn(checked)
  }

  const handleTimezoneChange = async (value: string) => {
    setTimezone(value)
    await updateTimezone(value)
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Settings</h1>

      <div className="card mb-6">
        <div className="p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">WhatsApp Notifications</h2>
          <div className="space-y-4">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={optIn}
                onChange={(e) => handleOptInChange(e.target.checked)}
                className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
              />
              <span className="text-gray-700">Enable WhatsApp notifications</span>
            </label>
            {user && !user.whatsappOptIn && (
              <p className="text-sm text-gray-500 ml-8">
                You'll need to opt-in via WhatsApp to receive notifications
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="card mb-6">
        <div className="p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Timezone</h2>
          <select
            value={timezone}
            onChange={(e) => handleTimezoneChange(e.target.value)}
            className="input max-w-xs"
          >
            <option value="Asia/Kolkata">India (IST)</option>
            <option value="Europe/London">London (GMT/BST)</option>
            <option value="America/New_York">New York (EST/EDT)</option>
            <option value="America/Los_Angeles">Los Angeles (PST/PDT)</option>
            <option value="Europe/Paris">Paris (CET/CEST)</option>
            <option value="Asia/Dubai">Dubai (GST)</option>
            <option value="Asia/Singapore">Singapore (SGT)</option>
            <option value="Australia/Sydney">Sydney (AEST/AEDT)</option>
          </select>
        </div>
      </div>

      <div className="card mb-6">
        <div className="p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Notification Preferences</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.matchStartReminder}
                  onChange={(e) => setFormData({ ...formData, matchStartReminder: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Match start reminders</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.goalNotifications}
                  onChange={(e) => setFormData({ ...formData, goalNotifications: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Goal notifications</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.cardNotifications}
                  onChange={(e) => setFormData({ ...formData, cardNotifications: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Yellow/Red card notifications</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.substitutionNotifications}
                  onChange={(e) => setFormData({ ...formData, substitutionNotifications: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Substitution notifications</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.halfTimeNotifications}
                  onChange={(e) => setFormData({ ...formData, halfTimeNotifications: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Half-time notifications</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.fullTimeNotifications}
                  onChange={(e) => setFormData({ ...formData, fullTimeNotifications: e.target.checked })}
                  className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <span className="text-gray-700">Full-time notifications</span>
              </label>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Remind me before match starts (minutes)
              </label>
              <input
                type="number"
                min="5"
                max="120"
                step="5"
                value={formData.reminderMinutesBefore}
                onChange={(e) => setFormData({ ...formData, reminderMinutesBefore: parseInt(e.target.value) || 15 })}
                className="input max-w-xs"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Quiet hours start</label>
                <input
                  type="time"
                  value={formData.quietHoursStart || ''}
                  onChange={(e) => setFormData({ ...formData, quietHoursStart: e.target.value || null })}
                  className="input"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Quiet hours end</label>
                <input
                  type="time"
                  value={formData.quietHoursEnd || ''}
                  onChange={(e) => setFormData({ ...formData, quietHoursEnd: e.target.value || null })}
                  className="input"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={updatePreferencesMutation.isPending}
              className="btn-primary"
            >
              {updatePreferencesMutation.isPending ? 'Saving...' : 'Save Preferences'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}