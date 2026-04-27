import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import {
  approveApproval,
  cancelBooking,
  cancelEvent,
  clearAuthSession,
  createBooking,
  createEvent,
  getEvents,
  getMyBookings,
  getMyEvents,
  getNotificationPreferences,
  getNotifications,
  getPendingApprovals,
  getProfile,
  getResources,
  getUnreadCount,
  login,
  markNotificationRead,
  publishEvent,
  readAccessToken,
  readStoredUser,
  register,
  updateNotificationPreferences,
  updateProfile,
  writeAuthSession,
  rejectApproval
} from './api';
import { Notice } from './components/Notice';
import { ProfilePanel } from './modules/account/ProfilePanel';
import { PreferencesPanel } from './modules/account/PreferencesPanel';
import { AuthPanel } from './modules/auth/AuthPanel';
import { BookingsPanel } from './modules/bookings/BookingsPanel';
import { EventsPanel } from './modules/events/EventsPanel';
import { NotificationsPanel } from './modules/notifications/NotificationsPanel';
import { ResourcesPanel } from './modules/resources/ResourcesPanel';
import { ApprovalsPanel } from './modules/workflows/ApprovalsPanel';
import type {
  ApprovalItem,
  AuthUser,
  BookingDraft,
  BookingItem,
  EventDraft,
  EventItem,
  NotificationPreference,
  NotificationItem,
  ResourceItem,
  UserProfile
} from './types';

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

const defaultBookingDraft: BookingDraft = {
  resourceId: '',
  linkedEventId: '',
  startAt: '',
  endAt: '',
  purpose: ''
};

type AuthMode = 'login' | 'register';
type SectionKey = 'account' | 'events' | 'resources' | 'bookings' | 'notifications' | 'approvals';

const sections: Array<{ key: SectionKey; label: string }> = [
  { key: 'account', label: 'Account' },
  { key: 'events', label: 'Events' },
  { key: 'resources', label: 'Resources' },
  { key: 'bookings', label: 'Bookings' },
  { key: 'notifications', label: 'Notifications' },
  { key: 'approvals', label: 'Approvals' }
];

export function App() {
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [section, setSection] = useState<SectionKey>('account');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [preferences, setPreferences] = useState<NotificationPreference>(defaultPreferences);
  const [profileDraft, setProfileDraft] = useState({ displayName: '', timezone: 'UTC' });
  const [publishedEvents, setPublishedEvents] = useState<EventItem[]>([]);
  const [myEvents, setMyEvents] = useState<EventItem[]>([]);
  const [resources, setResources] = useState<ResourceItem[]>([]);
  const [bookings, setBookings] = useState<BookingItem[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [approvals, setApprovals] = useState<ApprovalItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [eventDraft, setEventDraft] = useState<EventDraft>(defaultEventDraft);
  const [bookingDraft, setBookingDraft] = useState<BookingDraft>(defaultBookingDraft);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const authenticated = Boolean(readAccessToken() && currentUser);

  useEffect(() => {
    if (authenticated) {
      void loadWorkspace();
    }
  }, [authenticated]);

  async function loadWorkspace() {
    setError(null);
    try {
      const [profileResponse, preferencesResponse, allEvents, ownedEvents, resourceList, myBookingList, notificationList, unread, pendingApprovals] = await Promise.all([
        getProfile(),
        getNotificationPreferences(),
        getEvents(),
        getMyEvents(),
        getResources(),
        getMyBookings(),
        getNotifications(),
        getUnreadCount(),
        getPendingApprovals()
      ]);
      setProfile(profileResponse);
      setProfileDraft({ displayName: profileResponse.displayName, timezone: profileResponse.timezone });
      setPreferences(preferencesResponse);
      setPublishedEvents(allEvents);
      setMyEvents(ownedEvents);
      setResources(resourceList);
      setBookings(myBookingList);
      setNotifications(notificationList);
      setUnreadCount(unread.unreadCount);
      setApprovals(pendingApprovals);
    } catch (err) {
      setError(readError(err, 'Failed to load workspace data'));
    }
  }

  async function withFeedback(action: () => Promise<void>, successMessage: string) {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      await action();
      setMessage(successMessage);
    } catch (err) {
      setError(readError(err, 'Request failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await withFeedback(async () => {
      const response = authMode === 'login' ? await login(email, password) : await register(email, password);
      writeAuthSession(response);
      setCurrentUser(response.user);
      setPassword('');
    }, authMode === 'login' ? 'Signed in successfully.' : 'Account created successfully.');
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await withFeedback(async () => {
      const response = await updateProfile(profileDraft.displayName, profileDraft.timezone);
      setProfile(response);
    }, 'Profile updated.');
  }

  async function handlePreferenceSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await withFeedback(async () => {
      const response = await updateNotificationPreferences(preferences);
      setPreferences(response);
    }, 'Notification preferences updated.');
  }

  async function handleEventSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await withFeedback(async () => {
      await createEvent(eventDraft);
      setEventDraft(defaultEventDraft);
      await loadWorkspace();
    }, 'Event created.');
  }

  async function handleBookingSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await withFeedback(async () => {
      await createBooking(bookingDraft);
      setBookingDraft(defaultBookingDraft);
      await loadWorkspace();
    }, 'Booking created.');
  }

  function handleLogout() {
    clearAuthSession();
    setCurrentUser(null);
    setProfile(null);
    setPreferences(defaultPreferences);
    setPublishedEvents([]);
    setMyEvents([]);
    setResources([]);
    setBookings([]);
    setNotifications([]);
    setApprovals([]);
    setUnreadCount(0);
    setEventDraft(defaultEventDraft);
    setBookingDraft(defaultBookingDraft);
    setSection('account');
    setMessage('Signed out.');
    setError(null);
  }

  const initials = useMemo(() => {
    const source = profile?.displayName || currentUser?.email || 'TR';
    return source.slice(0, 2).toUpperCase();
  }, [currentUser?.email, profile?.displayName]);

  const rolesLabel = currentUser?.roles.join(', ') ?? 'Guest';

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
        </div>
        <nav className="nav-list" aria-label="Primary">
          {sections.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`nav-item ${section === item.key ? 'active' : ''}`}
              onClick={() => setSection(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Frontend workspace</p>
            <h2>{authenticated ? 'Service Console' : 'Authentication'}</h2>
          </div>
          {authenticated ? (
            <div className="account-chip">
              <span>{initials}</span>
              <div className="account-copy">
                <strong>{currentUser?.email}</strong>
                <small>{rolesLabel}</small>
              </div>
              <button type="button" className="secondary-button" onClick={handleLogout}>Sign out</button>
            </div>
          ) : null}
        </header>

        <Notice message={message} tone="success" />
        <Notice message={error} tone="error" />

        {!authenticated ? (
          <AuthPanel
            authMode={authMode}
            email={email}
            password={password}
            loading={loading}
            onModeChange={setAuthMode}
            onEmailChange={setEmail}
            onPasswordChange={setPassword}
            onSubmit={handleAuthSubmit}
          />
        ) : null}

        {authenticated && section === 'account' ? (
          <div className="stack-grid">
            <ProfilePanel
              profile={profile}
              draft={profileDraft}
              loading={loading}
              onChange={setProfileDraft}
              onSubmit={handleProfileSubmit}
            />
            <PreferencesPanel
              preferences={preferences}
              loading={loading}
              onChange={setPreferences}
              onSubmit={handlePreferenceSubmit}
            />
          </div>
        ) : null}

        {authenticated && section === 'events' ? (
          <EventsPanel
            publishedEvents={publishedEvents}
            myEvents={myEvents}
            draft={eventDraft}
            loading={loading}
            onDraftChange={setEventDraft}
            onSubmit={handleEventSubmit}
            onReload={() => void loadWorkspace()}
            onPublish={(eventId) => void withFeedback(async () => {
              await publishEvent(eventId);
              await loadWorkspace();
            }, 'Event published.')}
            onCancel={(eventId) => void withFeedback(async () => {
              await cancelEvent(eventId);
              await loadWorkspace();
            }, 'Event cancelled.')}
          />
        ) : null}

        {authenticated && section === 'resources' ? (
          <ResourcesPanel
            resources={resources}
            onReload={() => void loadWorkspace()}
            onUseResource={(resourceId) => {
              setBookingDraft((current) => ({ ...current, resourceId }));
              setSection('bookings');
              setMessage('Resource copied into the booking form.');
            }}
          />
        ) : null}

        {authenticated && section === 'bookings' ? (
          <BookingsPanel
            bookings={bookings}
            resources={resources}
            draft={bookingDraft}
            loading={loading}
            onDraftChange={setBookingDraft}
            onSubmit={handleBookingSubmit}
            onReload={() => void loadWorkspace()}
            onCancel={(bookingId) => void withFeedback(async () => {
              await cancelBooking(bookingId);
              await loadWorkspace();
            }, 'Booking cancelled.')}
          />
        ) : null}

        {authenticated && section === 'notifications' ? (
          <NotificationsPanel
            notifications={notifications}
            unreadCount={unreadCount}
            loading={loading}
            onReload={() => void loadWorkspace()}
            onMarkRead={(notificationId) => void withFeedback(async () => {
              await markNotificationRead(notificationId);
              await loadWorkspace();
            }, 'Notification marked as read.')}
          />
        ) : null}

        {authenticated && section === 'approvals' ? (
          <ApprovalsPanel
            approvals={approvals}
            loading={loading}
            onReload={() => void loadWorkspace()}
            onApprove={(approvalId) => void withFeedback(async () => {
              await approveApproval(approvalId);
              await loadWorkspace();
            }, 'Approval completed.')}
            onReject={(approvalId) => void withFeedback(async () => {
              await rejectApproval(approvalId);
              await loadWorkspace();
            }, 'Approval rejected.')}
          />
        ) : null}
      </section>
    </main>
  );
}

function readError(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}
