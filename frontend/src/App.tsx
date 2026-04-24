import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import {
  clearAuthSession,
  getNotificationPreferences,
  getProfile,
  login,
  readAccessToken,
  readStoredUser,
  register,
  writeAuthSession,
  updateNotificationPreferences,
  updateProfile
} from './api';
import type { AuthUser, NotificationPreference, UserProfile } from './types';

type AuthMode = 'login' | 'register';

const defaultPreferences: NotificationPreference = {
  inAppEnabled: true,
  emailEnabled: true,
  reminderMinutesBefore: 30
};

export function App() {
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [preferences, setPreferences] = useState<NotificationPreference>(defaultPreferences);
  const [profileDraft, setProfileDraft] = useState({ displayName: '', timezone: 'UTC' });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const authenticated = Boolean(readAccessToken() && currentUser);

  useEffect(() => {
    if (authenticated) {
      void loadUserData();
    }
  }, [authenticated]);

  async function loadUserData() {
    setError(null);
    try {
      const [profileResponse, preferencesResponse] = await Promise.all([
        getProfile(),
        getNotificationPreferences()
      ]);
      setProfile(profileResponse);
      setProfileDraft({
        displayName: profileResponse.displayName,
        timezone: profileResponse.timezone
      });
      setPreferences(preferencesResponse);
    } catch (err) {
      setError(readError(err, 'Failed to load user data'));
    }
  }

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      const response = authMode === 'login'
        ? await login(email, password)
        : await register(email, password);

      writeAuthSession(response);
      setCurrentUser(response.user);
      setMessage(authMode === 'login' ? 'Signed in successfully.' : 'Account created successfully.');
      setPassword('');
    } catch (err) {
      setError(readError(err, 'Authentication failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      const response = await updateProfile(profileDraft.displayName, profileDraft.timezone);
      setProfile(response);
      setMessage('Profile updated.');
    } catch (err) {
      setError(readError(err, 'Profile update failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handlePreferenceSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      const response = await updateNotificationPreferences(preferences);
      setPreferences(response);
      setMessage('Notification preferences updated.');
    } catch (err) {
      setError(readError(err, 'Preference update failed'));
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    clearAuthSession();
    setCurrentUser(null);
    setProfile(null);
    setPreferences(defaultPreferences);
    setMessage('Signed out.');
    setError(null);
  }

  const initials = useMemo(() => {
    const source = profile?.displayName || currentUser?.email || 'TR';
    return source.slice(0, 2).toUpperCase();
  }, [currentUser?.email, profile?.displayName]);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
        </div>
        <nav className="nav-list" aria-label="Primary">
          <span className="nav-item active">Identity</span>
          <span className="nav-item">Profiles</span>
          <span className="nav-item">Preferences</span>
        </nav>
      </aside>

      <section className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Auth and user service</p>
            <h2>Account Workspace</h2>
          </div>
          {authenticated ? (
            <div className="account-chip">
              <span>{initials}</span>
              <button type="button" onClick={handleLogout}>Sign out</button>
            </div>
          ) : null}
        </header>

        {message ? <div className="notice success">{message}</div> : null}
        {error ? <div className="notice error">{error}</div> : null}

        {!authenticated ? (
          <section className="panel auth-panel">
            <div>
              <p className="eyebrow">Session</p>
              <h3>{authMode === 'login' ? 'Sign in' : 'Create account'}</h3>
            </div>

            <div className="segmented-control" aria-label="Authentication mode">
              <button
                type="button"
                className={authMode === 'login' ? 'selected' : ''}
                onClick={() => setAuthMode('login')}
              >
                Login
              </button>
              <button
                type="button"
                className={authMode === 'register' ? 'selected' : ''}
                onClick={() => setAuthMode('register')}
              >
                Register
              </button>
            </div>

            <form className="form-grid" onSubmit={handleAuthSubmit}>
              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  minLength={8}
                  required
                />
              </label>
              <button className="primary-button" type="submit" disabled={loading}>
                {loading ? 'Processing...' : authMode === 'login' ? 'Sign in' : 'Create account'}
              </button>
            </form>
          </section>
        ) : (
          <div className="dashboard-grid">
            <section className="panel">
              <div>
                <p className="eyebrow">Current user</p>
                <h3>{currentUser?.email}</h3>
              </div>
              <dl className="detail-list">
                <div>
                  <dt>User ID</dt>
                  <dd>{currentUser?.userId}</dd>
                </div>
                <div>
                  <dt>Roles</dt>
                  <dd>{currentUser?.roles.join(', ')}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>{currentUser?.status}</dd>
                </div>
              </dl>
            </section>

            <section className="panel">
              <div>
                <p className="eyebrow">Profile</p>
                <h3>{profile?.displayName || 'Profile details'}</h3>
              </div>
              <form className="form-grid" onSubmit={handleProfileSubmit}>
                <label>
                  Display name
                  <input
                    value={profileDraft.displayName}
                    onChange={(event) => setProfileDraft((draft) => ({ ...draft, displayName: event.target.value }))}
                    required
                  />
                </label>
                <label>
                  Timezone
                  <input
                    value={profileDraft.timezone}
                    onChange={(event) => setProfileDraft((draft) => ({ ...draft, timezone: event.target.value }))}
                    required
                  />
                </label>
                <button className="primary-button" type="submit" disabled={loading}>
                  Save profile
                </button>
              </form>
            </section>

            <section className="panel">
              <div>
                <p className="eyebrow">Notifications</p>
                <h3>Delivery preferences</h3>
              </div>
              <form className="form-grid" onSubmit={handlePreferenceSubmit}>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={preferences.inAppEnabled}
                    onChange={(event) => setPreferences((value) => ({ ...value, inAppEnabled: event.target.checked }))}
                  />
                  In-app notifications
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={preferences.emailEnabled}
                    onChange={(event) => setPreferences((value) => ({ ...value, emailEnabled: event.target.checked }))}
                  />
                  Email notifications
                </label>
                <label>
                  Reminder lead time
                  <input
                    type="number"
                    min={0}
                    max={10080}
                    value={preferences.reminderMinutesBefore}
                    onChange={(event) => setPreferences((value) => ({
                      ...value,
                      reminderMinutesBefore: Number(event.target.value)
                    }))}
                  />
                </label>
                <button className="primary-button" type="submit" disabled={loading}>
                  Save preferences
                </button>
              </form>
            </section>
          </div>
        )}
      </section>
    </main>
  );
}

function readError(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}
