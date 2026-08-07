import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Header from '../components/Header.jsx'
import * as profileService from '../services/profileService.js'
import { apiOrigin } from '../services/api.js'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

export default function Profile() {
  const { updateUser, logout } = useAuth()
  const navigate = useNavigate()

  const [profile, setProfile] = useState(null)
  const [loadError, setLoadError] = useState(null)

  const [profileForm, setProfileForm] = useState({ fullName: '', phoneNumber: '' })
  const [profileStatus, setProfileStatus] = useState({ error: null, success: null, submitting: false })

  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '' })
  const [passwordStatus, setPasswordStatus] = useState({ error: null, success: null, submitting: false })

  const [pictureStatus, setPictureStatus] = useState({ error: null, submitting: false })

  const [deletePassword, setDeletePassword] = useState('')
  const [deleteStatus, setDeleteStatus] = useState({ error: null, submitting: false, confirming: false })

  useEffect(() => {
    profileService
      .getProfile()
      .then((data) => {
        setProfile(data)
        setProfileForm({ fullName: data.fullName, phoneNumber: data.phoneNumber })
        updateUser(data)
      })
      .catch(() => setLoadError('Could not load your profile. Try refreshing the page.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleProfileSubmit(event) {
    event.preventDefault()
    setProfileStatus({ error: null, success: null, submitting: true })
    try {
      const updated = await profileService.updateProfile(profileForm)
      setProfile(updated)
      updateUser(updated)
      setProfileStatus({ error: null, success: 'Saved.', submitting: false })
    } catch (err) {
      setProfileStatus({
        error: err.response?.data?.message ?? 'Could not save your profile.',
        success: null,
        submitting: false,
      })
    }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setPasswordStatus({ error: null, success: null, submitting: true })
    try {
      await profileService.changePassword(passwordForm)
      setPasswordForm({ currentPassword: '', newPassword: '' })
      setPasswordStatus({
        error: null,
        success: 'Password changed. Other sessions have been signed out.',
        submitting: false,
      })
    } catch (err) {
      setPasswordStatus({
        error: err.response?.data?.message ?? 'Could not change your password.',
        success: null,
        submitting: false,
      })
    }
  }

  async function handlePictureChange(event) {
    const file = event.target.files?.[0]
    if (!file) return
    setPictureStatus({ error: null, submitting: true })
    try {
      const updated = await profileService.uploadProfilePicture(file)
      setProfile(updated)
      updateUser(updated)
      setPictureStatus({ error: null, submitting: false })
    } catch (err) {
      setPictureStatus({
        error: err.response?.data?.message ?? 'Could not upload that image.',
        submitting: false,
      })
    } finally {
      event.target.value = ''
    }
  }

  async function handleDelete(event) {
    event.preventDefault()
    setDeleteStatus((prev) => ({ ...prev, error: null, submitting: true }))
    try {
      await profileService.deleteAccount(deletePassword)
      await logout()
      navigate('/')
    } catch (err) {
      setDeleteStatus({
        error: err.response?.data?.message ?? 'Could not delete your account.',
        submitting: false,
        confirming: true,
      })
    }
  }

  if (loadError) {
    return (
      <div className="min-h-screen flex flex-col">
        <Header />
        <div className="flex-1 flex items-center justify-center px-6">
          <p className="font-mono text-sm text-debit">{loadError}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14">
        <div className="mx-auto max-w-lg space-y-12">
          <div>
            <p className="font-mono text-xs uppercase tracking-widest text-stamp">Account holder</p>
            <h1 className="font-display text-2xl font-semibold mt-2">Your ledger details</h1>
          </div>

          <section className="flex items-center gap-5">
            <div className="w-16 h-16 rounded-full bg-ink-soft border border-ink-text/15 overflow-hidden flex items-center justify-center shrink-0">
              {profile?.profilePictureUrl ? (
                <img
                  src={`${apiOrigin}${profile.profilePictureUrl}`}
                  alt="Profile"
                  className="w-full h-full object-cover"
                />
              ) : (
                <span className="font-display text-xl text-ink-text-dim">
                  {profile?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
                </span>
              )}
            </div>
            <div>
              <label className="inline-block font-mono text-xs text-credit underline underline-offset-4 cursor-pointer">
                {pictureStatus.submitting ? 'Uploading…' : 'Change photo'}
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  className="hidden"
                  onChange={handlePictureChange}
                  disabled={pictureStatus.submitting}
                />
              </label>
              <p className="font-mono text-xs text-ink-text-dim mt-1">JPEG or PNG, up to 2MB</p>
              {pictureStatus.error && <p className="font-mono text-xs text-debit mt-1">{pictureStatus.error}</p>}
            </div>
          </section>

          <section>
            <p className="font-mono text-xs uppercase tracking-widest text-ink-text-dim mb-4">Edit profile</p>
            <form onSubmit={handleProfileSubmit} className="space-y-4">
              <div>
                <label htmlFor="fullName" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Full name
                </label>
                <input
                  id="fullName"
                  type="text"
                  required
                  value={profileForm.fullName}
                  onChange={(e) => setProfileForm((prev) => ({ ...prev, fullName: e.target.value }))}
                  className={inputClass}
                />
              </div>
              <div>
                <label htmlFor="phoneNumber" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Phone number
                </label>
                <input
                  id="phoneNumber"
                  type="tel"
                  required
                  value={profileForm.phoneNumber}
                  onChange={(e) => setProfileForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                  className={inputClass}
                />
              </div>
              <div>
                <span className="block font-mono text-xs text-ink-text-dim mb-1">Email</span>
                <p className="font-mono text-sm text-ink-text-dim/70 py-2">{profile?.email} (cannot be changed)</p>
              </div>

              {profileStatus.error && <p className="font-mono text-xs text-debit">{profileStatus.error}</p>}
              {profileStatus.success && <p className="font-mono text-xs text-credit">{profileStatus.success}</p>}

              <button
                type="submit"
                disabled={profileStatus.submitting}
                className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
              >
                {profileStatus.submitting ? 'Saving…' : 'Save changes'}
              </button>
            </form>
          </section>

          <section>
            <p className="font-mono text-xs uppercase tracking-widest text-ink-text-dim mb-4">Change password</p>
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div>
                <label htmlFor="currentPassword" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Current password
                </label>
                <input
                  id="currentPassword"
                  type="password"
                  required
                  autoComplete="current-password"
                  value={passwordForm.currentPassword}
                  onChange={(e) => setPasswordForm((prev) => ({ ...prev, currentPassword: e.target.value }))}
                  className={inputClass}
                />
              </div>
              <div>
                <label htmlFor="newPassword" className="block font-mono text-xs text-ink-text-dim mb-1">
                  New password
                </label>
                <input
                  id="newPassword"
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm((prev) => ({ ...prev, newPassword: e.target.value }))}
                  className={inputClass}
                />
              </div>

              {passwordStatus.error && <p className="font-mono text-xs text-debit">{passwordStatus.error}</p>}
              {passwordStatus.success && <p className="font-mono text-xs text-credit">{passwordStatus.success}</p>}

              <button
                type="submit"
                disabled={passwordStatus.submitting}
                className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
              >
                {passwordStatus.submitting ? 'Updating…' : 'Update password'}
              </button>
            </form>
          </section>

          <section className="border border-debit/30 rounded-lg p-5">
            <p className="font-mono text-xs uppercase tracking-widest text-debit mb-2">Danger zone</p>
            <p className="text-sm text-ink-text-dim mb-4">
              Deleting your account closes this ledger. This can't be undone from the app.
            </p>

            {!deleteStatus.confirming ? (
              <button
                type="button"
                onClick={() => setDeleteStatus((prev) => ({ ...prev, confirming: true }))}
                className="font-mono text-xs text-debit underline underline-offset-4"
              >
                Delete account
              </button>
            ) : (
              <form onSubmit={handleDelete} className="space-y-3">
                <label htmlFor="deletePassword" className="block font-mono text-xs text-ink-text-dim">
                  Enter your password to confirm
                </label>
                <input
                  id="deletePassword"
                  type="password"
                  required
                  value={deletePassword}
                  onChange={(e) => setDeletePassword(e.target.value)}
                  className={inputClass}
                />
                {deleteStatus.error && <p className="font-mono text-xs text-debit">{deleteStatus.error}</p>}
                <div className="flex gap-3">
                  <button
                    type="submit"
                    disabled={deleteStatus.submitting}
                    className="rounded-md bg-debit text-ink-text font-display text-sm font-semibold py-2 px-4 disabled:opacity-60"
                  >
                    {deleteStatus.submitting ? 'Deleting…' : 'Confirm delete'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleteStatus({ error: null, submitting: false, confirming: false })}
                    className="font-mono text-xs text-ink-text-dim"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
