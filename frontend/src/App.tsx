import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  ApiError,
  approveApproval,
  cancelBooking,
  cancelEvent,
  cancelEventRegistration,
  checkInEventRegistration,
  clearAuthSession,
  createBooking,
  createEvent,
  getDashboardOverview,
  getEventRegistrations,
  getEvents,
  getMyBookings,
  getMyEventRegistrations,
  getMyEvents,
  getNotificationPreferences,
  getNotifications,
  getPendingApprovals,
  getPopularResources,
  getProfile,
  getResources,
  getUnreadCount,
  googleAuthorizeUrl,
  login,
  markNotificationRead,
  publishEvent,
  readAccessToken,
  readStoredUser,
  register,
  registerForEvent,
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
import { DashboardPanel } from './modules/dashboard/DashboardPanel';
import { EventsPanel } from './modules/events/EventsPanel';
import { NotificationsPanel } from './modules/notifications/NotificationsPanel';
import { ResourcesPanel } from './modules/resources/ResourcesPanel';
import { ApprovalsPanel } from './modules/workflows/ApprovalsPanel';
import type {
  ApprovalItem,
  AuthResponse,
  AuthUser,
  BookingDraft,
  BookingItem,
  DashboardOverview,
  EventDraft,
  EventItem,
  EventRegistrationItem,
  NotificationItem,
  NotificationPreference,
  ResourceItem,
  ResourcePopularityItem,
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

const defaultDashboardOverview: DashboardOverview = {
  totalBookings: 0,
  approvedBookings: 0,
  pendingApprovals: 0,
  waitlistedBookings: 0,
  cancelledOrRejectedBookings: 0,
  uniqueResourcesUsed: 0,
  nextSevenDaysApprovedBookings: 0,
  totalApprovedReservedMinutes: 0
};

type AuthMode = 'login' | 'register';

type NavItem = {
  label: string;
  path: string;
  roles?: string[];
  badge?: number;
};

const googleEnabled = (import.meta.env.VITE_GOOGLE_OAUTH_ENABLED ?? 'false') === 'true';

export function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [preferences, setPreferences] = useState<NotificationPreference>(defaultPreferences);
  const [profileDraft, setProfileDraft] = useState({ displayName: '', timezone: 'UTC' });
  const [publishedEvents, setPublishedEvents] = useState<EventItem[]>([]);
  const [myEvents, setMyEvents] = useState<EventItem[]>([]);
  const [myEventRegistrations, setMyEventRegistrations] = useState<EventRegistrationItem[]>([]);
  const [managedRegistrations, setManagedRegistrations] = useState<EventRegistrationItem[]>([]);
  const [selectedManagedEventId, setSelectedManagedEventId] = useState('');
  const [resources, setResources] = useState<ResourceItem[]>([]);
  const [bookings, setBookings] = useState<BookingItem[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [approvals, setApprovals] = useState<ApprovalItem[]>([]);
  const [dashboardOverview, setDashboardOverview] = useState<DashboardOverview>(defaultDashboardOverview);
  const [popularResources, setPopularResources] = useState<ResourcePopularityItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [eventDraft, setEventDraft] = useState<EventDraft>(defaultEventDraft);
  const [bookingDraft, setBookingDraft] = useState<BookingDraft>(defaultBookingDraft);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [oauthHandling, setOauthHandling] = useState(false);

  const authenticated = Boolean(readAccessToken() && currentUser);
  const canReviewApprovals = hasAnyRole(currentUser, ['ADMIN', 'RESOURCE_MANAGER']);

  useEffect(() => {
    if (location.pathname === '/oauth2/callback') {
      void handleOAuth2Callback();
    }
  }, [location.pathname]);

  useEffect(() => {
    if (authenticated) {
      void loadWorkspace();
      if (location.pathname === '/' || location.pathname === '/login') {
        navigate('/dashboard', { replace: true });
      }
    }
  }, [authenticated]);

  async function handleOAuth2Callback() {
    setOauthHandling(true);
    setError(null);
    setMessage(null);
    try {
      const params = new URLSearchParams(location.search);
      const accessToken = params.get('accessToken');
      const emailValue = params.get('email');
      const userId = params.get('userId');
      const roles = (params.get('roles') ?? 'USER').split(',').filter(Boolean);
      const tokenType = params.get('tokenType') ?? 'Bearer';
      const expiresIn = Number(params.get('expiresIn') ?? '3600');
      const oauthError = params.get('error');

      if (oauthError) {
        throw new Error(oauthError);
      }
      if (!accessToken || !emailValue || !userId) {
        throw new Error('Missing OAuth2 callback payload');
      }

      const session: AuthResponse = {
        tokens: {
          accessToken,
          tokenType,
          expiresInSeconds: expiresIn
        },
        user: {
          userId,
          email: emailValue,
          roles,
          status: 'ACTIVE'
        }
      };
      writeAuthSession(session);
      setCurrentUser(session.user);
      navigate('/dashboard', { replace: true });
      setMessage('Google sign-in completed.');
    } catch (callbackError) {
      setError(readError(callbackError, 'Google sign-in failed'));
      navigate('/login', { replace: true });
    } finally {
      setOauthHandling(false);
    }
  }

  async function loadWorkspace() {
    setError(null);
    try {
      const [
        profileResponse,
        preferencesResponse,
        allEvents,
        ownedEvents,
        registrations,
        resourceList,
        myBookingList,
        notificationList,
        unread,
        dashboard,
        popular,
        pendingApprovals
      ] = await Promise.all([
        getProfile(),
        getNotificationPreferences(),
        getEvents(),
        getMyEvents(),
        getMyEventRegistrations(),
        getResources(),
        getMyBookings(),
        getNotifications(),
        getUnreadCount(),
        safeOptional(() => getDashboardOverview(), defaultDashboardOverview),
        safeOptional(() => getPopularResources(), [] as ResourcePopularityItem[]),
        canReviewApprovals ? safeOptional(() => getPendingApprovals(), [] as ApprovalItem[]) : Promise.resolve([] as ApprovalItem[])
      ]);

      setProfile(profileResponse);
      setProfileDraft({ displayName: profileResponse.displayName, timezone: profileResponse.timezone });
      setPreferences(preferencesResponse);
      setPublishedEvents(allEvents);
      setMyEvents(ownedEvents);
      setMyEventRegistrations(registrations);
      setResources(resourceList);
      setBookings(myBookingList);
      setNotifications(notificationList);
      setUnreadCount(unread.unreadCount);
      setDashboardOverview(dashboard);
      setPopularResources(popular);
      setApprovals(pendingApprovals);

      const nextManagedEventId = ownedEvents.some((item) => item.eventId === selectedManagedEventId)
        ? selectedManagedEventId
        : (ownedEvents[0]?.eventId ?? '');
      setSelectedManagedEventId(nextManagedEventId);
      if (nextManagedEventId) {
        const registrationList = await safeOptional(() => getEventRegistrations(nextManagedEventId), [] as EventRegistrationItem[]);
        setManagedRegistrations(registrationList);
      } else {
        setManagedRegistrations([]);
      }
    } catch (workspaceError) {
      handleRuntimeError(workspaceError, 'Failed to load workspace data');
    }
  }

  async function loadManagedRegistrations(eventId: string) {
    if (!eventId) {
      setManagedRegistrations([]);
      return;
    }
    try {
      const registrationList = await getEventRegistrations(eventId);
      setManagedRegistrations(registrationList);
    } catch (requestError) {
      handleRuntimeError(requestError, 'Failed to load event registrations');
    }
  }

  async function withFeedback(action: () => Promise<void>, successMessage: string) {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      await action();
      setMessage(successMessage);
    } catch (requestError) {
      handleRuntimeError(requestError, 'Request failed');
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
      navigate('/dashboard', { replace: true });
    }, authMode === 'login' ? 'Signed in successfully.' : 'Account created successfully.');
  }

  function handleGoogleLogin() {
    window.location.assign(googleAuthorizeUrl());
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
    setMyEventRegistrations([]);
    setManagedRegistrations([]);
    setResources([]);
    setBookings([]);
    setNotifications([]);
    setApprovals([]);
    setDashboardOverview(defaultDashboardOverview);
    setPopularResources([]);
    setUnreadCount(0);
    setEventDraft(defaultEventDraft);
    setBookingDraft(defaultBookingDraft);
    setSelectedManagedEventId('');
    setMessage('Signed out.');
    setError(null);
    navigate('/login', { replace: true });
  }

  function handleRuntimeError(value: unknown, fallback: string) {
    if (value instanceof ApiError && value.status === 401) {
      handleLogout();
      return;
    }
    setError(readError(value, fallback));
  }

  const initials = useMemo(() => {
    const source = profile?.displayName || currentUser?.email || 'TR';
    return source.slice(0, 2).toUpperCase();
  }, [currentUser?.email, profile?.displayName]);

  const rolesLabel = currentUser?.roles.join(', ') ?? 'Guest';

  const navigationItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'Account', path: '/account' },
    { label: 'Events', path: '/events' },
    { label: 'Resources', path: '/resources' },
    { label: 'Bookings', path: '/bookings' },
    { label: 'Notifications', path: '/notifications', badge: unreadCount },
    { label: 'Approvals', path: '/approvals', roles: ['ADMIN', 'RESOURCE_MANAGER'] }
  ].filter((item) => !item.roles || hasAnyRole(currentUser, item.roles));

  if (location.pathname === '/oauth2/callback') {
    return (
      <main className="auth-shell">
        <section className="panel auth-panel">
          <p className="eyebrow">OAuth2</p>
          <h3>{oauthHandling ? 'Completing sign-in' : 'Redirecting'}</h3>
          <p className="helper-copy">The platform is processing the Google sign-in response.</p>
        </section>
      </main>
    );
  }

  if (!authenticated) {
    return (
      <main className="auth-shell">
        <section className="auth-column">
          <div className="auth-copy">
            <p className="eyebrow">Team Resource</p>
            <h1>Management Console</h1>
            <p className="helper-copy">Unified workspace for event coordination, resource booking, approvals, notifications, and analytics.</p>
          </div>
          <Notice message={message} tone="success" />
          <Notice message={error} tone="error" />
          <AuthPanel
            authMode={authMode}
            email={email}
            password={password}
            loading={loading}
            googleEnabled={googleEnabled}
            onModeChange={setAuthMode}
            onEmailChange={setEmail}
            onPasswordChange={setPassword}
            onSubmit={handleAuthSubmit}
            onGoogleLogin={handleGoogleLogin}
          />
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
        </div>
        <nav className="nav-list" aria-label="Primary">
          {navigationItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <span>{item.label}</span>
              {item.badge && item.badge > 0 ? <small>{item.badge}</small> : null}
            </NavLink>
          ))}
        </nav>
      </aside>

      <section className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Frontend workspace</p>
            <h2>{resolveTitle(location.pathname)}</h2>
          </div>
          <div className="account-chip">
            <span>{initials}</span>
            <div className="account-copy">
              <strong>{currentUser?.email}</strong>
              <small>{rolesLabel}</small>
            </div>
            <button type="button" className="secondary-button" onClick={handleLogout}>Sign out</button>
          </div>
        </header>

        <Notice message={message} tone="success" />
        <Notice message={error} tone="error" />

        <Routes>
          <Route
            path="/dashboard"
            element={<DashboardPanel overview={dashboardOverview} popularResources={popularResources} onReload={() => void loadWorkspace()} />}
          />
          <Route
            path="/account"
            element={(
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
            )}
          />
          <Route
            path="/events"
            element={(
              <EventsPanel
                publishedEvents={publishedEvents}
                myEvents={myEvents}
                myRegistrations={myEventRegistrations}
                managedRegistrations={managedRegistrations}
                selectedManagedEventId={selectedManagedEventId}
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
                onRegister={(eventId) => void withFeedback(async () => {
                  await registerForEvent(eventId);
                  await loadWorkspace();
                }, 'Event registration created.')}
                onCancelRegistration={(eventId) => void withFeedback(async () => {
                  await cancelEventRegistration(eventId);
                  await loadWorkspace();
                }, 'Event registration cancelled.')}
                onManagedEventChange={(eventId) => {
                  setSelectedManagedEventId(eventId);
                  void loadManagedRegistrations(eventId);
                }}
                onCheckIn={(eventId, registrationId) => void withFeedback(async () => {
                  await checkInEventRegistration(eventId, registrationId);
                  await loadManagedRegistrations(eventId);
                  await loadWorkspace();
                }, 'Attendee checked in.')}
              />
            )}
          />
          <Route
            path="/resources"
            element={<ResourcesPanel resources={resources} onReload={() => void loadWorkspace()} onUseResource={(resourceId) => {
              setBookingDraft((current) => ({ ...current, resourceId }));
              navigate('/bookings');
            }} />}
          />
          <Route
            path="/bookings"
            element={<BookingsPanel
              bookings={bookings}
              resources={resources}
              events={publishedEvents}
              draft={bookingDraft}
              loading={loading}
              onDraftChange={setBookingDraft}
              onSubmit={handleBookingSubmit}
              onReload={() => void loadWorkspace()}
              onCancel={(bookingId) => void withFeedback(async () => {
                await cancelBooking(bookingId);
                await loadWorkspace();
              }, 'Booking cancelled.')}
            />}
          />
          <Route
            path="/notifications"
            element={<NotificationsPanel
              notifications={notifications}
              unreadCount={unreadCount}
              loading={loading}
              onReload={() => void loadWorkspace()}
              onMarkRead={(notificationId) => void withFeedback(async () => {
                await markNotificationRead(notificationId);
                await loadWorkspace();
              }, 'Notification marked as read.')}
            />}
          />
          <Route
            path="/approvals"
            element={canReviewApprovals ? (
              <ApprovalsPanel
                approvals={approvals}
                loading={loading}
                onReload={() => void loadWorkspace()}
                onApprove={(approvalId) => void withFeedback(async () => {
                  await approveApproval(approvalId, 'Approved from frontend console');
                  await loadWorkspace();
                }, 'Approval completed.')}
                onReject={(approvalId) => void withFeedback(async () => {
                  await rejectApproval(approvalId, 'Rejected from frontend console');
                  await loadWorkspace();
                }, 'Approval rejected.')}
              />
            ) : (
              <section className="panel">
                <p className="eyebrow">Workflow</p>
                <h3>Access restricted</h3>
                <p className="helper-copy">Approvals are visible only to resource managers and administrators.</p>
              </section>
            )}
          />
          <Route path="/login" element={<Navigate to="/dashboard" replace />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </section>
    </main>
  );
}

async function safeOptional<T>(work: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await work();
  } catch (error) {
    if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
      return fallback;
    }
    throw error;
  }
}

function hasAnyRole(user: AuthUser | null, roles: string[]) {
  if (!user) {
    return false;
  }
  return roles.some((role) => user.roles.includes(role));
}

function readError(value: unknown, fallback: string) {
  if (value instanceof Error) {
    return value.message;
  }
  return fallback;
}

function resolveTitle(pathname: string) {
  switch (pathname) {
    case '/dashboard':
      return 'Dashboard';
    case '/account':
      return 'Account';
    case '/events':
      return 'Events';
    case '/resources':
      return 'Resources';
    case '/bookings':
      return 'Bookings';
    case '/notifications':
      return 'Notifications';
    case '/approvals':
      return 'Approvals';
    default:
      return 'Service Console';
  }
}
