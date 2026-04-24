import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import {
  clearAuthSession,
  createEvent,
  getEvents,
  getMyEvents,
  getNotificationPreferences,
  getProfile,
  login,
  publishEvent,
  readAccessToken,
  readStoredUser,
  register,
  cancelEvent,
  writeAuthSession,
  updateNotificationPreferences,
  updateProfile
} from './api';
import type { AuthUser, EventDraft, EventItem, NotificationPreference, UserProfile } from './types';

type AuthMode = 'login' | 'register';

const eventCategories = ['WORKSHOP', 'MEETING', 'SEMINAR', 'SOCIAL', 'TRAINING', 'OTHER'] as const;

const defaultPreferences: NotificationPreference = {
  inAppEnabled: true,
  emailEnabled: true,
  reminderMinutesBefore: 30
};

const defaultEventDraft: EventDraft = {
  title: '',
  description: '',
  category: 'WORKSHOP',
  location: '',
  capacity: 30,
  registrationOpenAt: '',
  registrationCloseAt: '',
  startAt: '',
  endAt: ''
};

export function App() {
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [preferences, setPreferences] = useState<NotificationPreference>(defaultPreferences);
  const [profileDraft, setProfileDraft] = useState({ displayName: '', timezone: 'UTC' });
  const [publishedEvents, setPublishedEvents] = useState<EventItem[]>([]);
  const [myEvents, setMyEvents] = useState<EventItem[]>([]);
  const [eventDraft, setEventDraft] = useState<EventDraft>(defaultEventDraft);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const authenticated = Boolean(readAccessToken() && currentUser);

  useEffect(() => {
    if (authenticated) {
      void loadUserData();
      void loadEventData();
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

  async function loadEventData() {
    try {
      const [allEvents, ownedEvents] = await Promise.all([getEvents(), getMyEvents()]);
      setPublishedEvents(allEvents);
      setMyEvents(ownedEvents);
    } catch (err) {
      setError(readError(err, 'Failed to load event data'));
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

  async function handleEventSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      await createEvent(eventDraft);
      setEventDraft(defaultEventDraft);
      await loadEventData();
      setMessage('Event created.');
    } catch (err) {
      setError(readError(err, 'Event creation failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handlePublishEvent(eventId: string) {
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      await publishEvent(eventId);
      await loadEventData();
      setMessage('Event published.');
    } catch (err) {
      setError(readError(err, 'Event publish failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handleCancelEvent(eventId: string) {
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      await cancelEvent(eventId);
      await loadEventData();
      setMessage('Event cancelled.');
    } catch (err) {
      setError(readError(err, 'Event cancellation failed'));
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    clearAuthSession();
    setCurrentUser(null);
    setProfile(null);
    setPreferences(defaultPreferences);
    setPublishedEvents([]);
    setMyEvents([]);
    setEventDraft(defaultEventDraft);
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
          <span className="nav-item">Events</span>
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

            <section className="panel panel-wide">
              <div>
                <p className="eyebrow">Event workspace</p>
                <h3>Create event</h3>
              </div>
              <form className="form-grid form-grid-two-columns" onSubmit={handleEventSubmit}>
                <label>
                  Title
                  <input
                    value={eventDraft.title}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, title: event.target.value }))}
                    minLength={3}
                    maxLength={120}
                    required
                  />
                </label>
                <label>
                  Category
                  <select
                    value={eventDraft.category}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, category: event.target.value }))}
                  >
                    {eventCategories.map((category) => (
                      <option key={category} value={category}>
                        {category}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="span-two">
                  Description
                  <textarea
                    value={eventDraft.description}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, description: event.target.value }))}
                    rows={4}
                    maxLength={2000}
                  />
                </label>
                <label>
                  Location
                  <input
                    value={eventDraft.location}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, location: event.target.value }))}
                    maxLength={180}
                    required
                  />
                </label>
                <label>
                  Capacity
                  <input
                    type="number"
                    min={1}
                    max={100000}
                    value={eventDraft.capacity}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, capacity: Number(event.target.value) }))}
                    required
                  />
                </label>
                <label>
                  Registration opens
                  <input
                    type="datetime-local"
                    value={eventDraft.registrationOpenAt}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, registrationOpenAt: event.target.value }))}
                  />
                </label>
                <label>
                  Registration closes
                  <input
                    type="datetime-local"
                    value={eventDraft.registrationCloseAt}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, registrationCloseAt: event.target.value }))}
                  />
                </label>
                <label>
                  Starts at
                  <input
                    type="datetime-local"
                    value={eventDraft.startAt}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, startAt: event.target.value }))}
                    required
                  />
                </label>
                <label>
                  Ends at
                  <input
                    type="datetime-local"
                    value={eventDraft.endAt}
                    onChange={(event) => setEventDraft((draft) => ({ ...draft, endAt: event.target.value }))}
                    required
                  />
                </label>
                <button className="primary-button" type="submit" disabled={loading}>
                  Create event
                </button>
              </form>
            </section>

            <section className="panel">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Published events</p>
                  <h3>Discovery feed</h3>
                </div>
                <button className="secondary-button" type="button" onClick={() => void loadEventData()} disabled={loading}>
                  Refresh
                </button>
              </div>
              <div className="stack-list">
                {publishedEvents.length === 0 ? (
                  <p className="empty-state">No published events are available yet.</p>
                ) : (
                  publishedEvents.map((item) => (
                    <article key={item.eventId} className="list-card">
                      <div className="list-card-header">
                        <div>
                          <h4>{item.title}</h4>
                          <p>{item.category} · {item.location}</p>
                        </div>
                        <span className="status-badge">{item.status}</span>
                      </div>
                      <p className="card-copy">{item.description || 'No description provided.'}</p>
                      <dl className="mini-detail-list">
                        <div>
                          <dt>Capacity</dt>
                          <dd>{item.capacity}</dd>
                        </div>
                        <div>
                          <dt>Starts</dt>
                          <dd>{formatDateTime(item.startAt)}</dd>
                        </div>
                        <div>
                          <dt>Ends</dt>
                          <dd>{formatDateTime(item.endAt)}</dd>
                        </div>
                      </dl>
                    </article>
                  ))
                )}
              </div>
            </section>

            <section className="panel">
              <div>
                <p className="eyebrow">My events</p>
                <h3>Organizer control</h3>
              </div>
              <div className="stack-list">
                {myEvents.length === 0 ? (
                  <p className="empty-state">Create your first event to see it here.</p>
                ) : (
                  myEvents.map((item) => (
                    <article key={item.eventId} className="list-card">
                      <div className="list-card-header">
                        <div>
                          <h4>{item.title}</h4>
                          <p>{item.category} · {formatDateTime(item.startAt)}</p>
                        </div>
                        <span className="status-badge">{item.status}</span>
                      </div>
                      <p className="card-copy">{item.location}</p>
                      <div className="action-row">
                        {item.status === 'DRAFT' ? (
                          <button
                            className="secondary-button"
                            type="button"
                            onClick={() => void handlePublishEvent(item.eventId)}
                            disabled={loading}
                          >
                            Publish
                          </button>
                        ) : null}
                        {item.status !== 'CANCELLED' ? (
                          <button
                            className="secondary-button danger-button"
                            type="button"
                            onClick={() => void handleCancelEvent(item.eventId)}
                            disabled={loading}
                          >
                            Cancel
                          </button>
                        ) : null}
                      </div>
                    </article>
                  ))
                )}
              </div>
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

function formatDateTime(value: string | null) {
  if (!value) {
    return 'Not set';
  }

  return new Date(value).toLocaleString();
}
