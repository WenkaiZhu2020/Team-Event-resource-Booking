import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { PropsWithChildren } from 'react';
import { useNavigate } from 'react-router-dom';
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
  rejectApproval,
  updateNotificationPreferences,
  updateProfile,
  writeAuthSession
} from '../../api';
import { hasAnyRole } from '../../shared/constants/roles';
import { navItems, type NavItem } from '../../shared/layout/navigation';
import { readError } from '../../shared/utils/error';
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
} from '../../types';

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

const googleEnabled = (import.meta.env.VITE_GOOGLE_OAUTH_ENABLED ?? 'false') === 'true';

type AuthMode = 'login' | 'register';

type ProfileDraft = {
  displayName: string;
  timezone: string;
};

interface AppContextValue {
  authMode: AuthMode;
  email: string;
  password: string;
  currentUser: AuthUser | null;
  profile: UserProfile | null;
  preferences: NotificationPreference;
  profileDraft: ProfileDraft;
  publishedEvents: EventItem[];
  myEvents: EventItem[];
  myEventRegistrations: EventRegistrationItem[];
  managedRegistrations: EventRegistrationItem[];
  managedEventId: string;
  resources: ResourceItem[];
  bookings: BookingItem[];
  notifications: NotificationItem[];
  approvals: ApprovalItem[];
  dashboardOverview: DashboardOverview;
  popularResources: ResourcePopularityItem[];
  unreadCount: number;
  eventDraft: EventDraft;
  bookingDraft: BookingDraft;
  loading: boolean;
  message: string | null;
  error: string | null;
  oauthHandling: boolean;
  googleEnabled: boolean;
  authenticated: boolean;
  canReviewApprovals: boolean;
  initials: string;
  rolesLabel: string;
  navigationItems: NavItem[];
  setAuthMode: (mode: AuthMode) => void;
  setEmail: (value: string) => void;
  setPassword: (value: string) => void;
  setProfileDraft: (draft: ProfileDraft) => void;
  setPreferences: (preferences: NotificationPreference) => void;
  setEventDraft: (draft: EventDraft) => void;
  setBookingDraft: (draft: BookingDraft) => void;
  submitAuth: () => Promise<void>;
  startGoogleLogin: () => void;
  completeOAuthCallback: (search: string) => Promise<void>;
  submitProfile: () => Promise<void>;
  submitPreferences: () => Promise<void>;
  createEventAction: () => Promise<void>;
  createBookingAction: () => Promise<void>;
  reloadWorkspace: () => Promise<void>;
  publishEventAction: (eventId: string) => Promise<void>;
  cancelEventAction: (eventId: string) => Promise<void>;
  registerForEventAction: (eventId: string) => Promise<void>;
  cancelEventRegistrationAction: (eventId: string) => Promise<void>;
  changeManagedEvent: (eventId: string) => Promise<void>;
  checkInRegistrationAction: (eventId: string, registrationId: string) => Promise<void>;
  cancelBookingAction: (bookingId: string) => Promise<void>;
  markNotificationReadAction: (notificationId: string) => Promise<void>;
  approveAction: (approvalId: string) => Promise<void>;
  rejectAction: (approvalId: string) => Promise<void>;
  seedBookingResource: (resourceId: string) => void;
  logout: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: PropsWithChildren) {
  const navigate = useNavigate();
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [preferences, setPreferences] = useState<NotificationPreference>(defaultPreferences);
  const [profileDraft, setProfileDraft] = useState<ProfileDraft>({ displayName: '', timezone: 'UTC' });
  const [publishedEvents, setPublishedEvents] = useState<EventItem[]>([]);
  const [myEvents, setMyEvents] = useState<EventItem[]>([]);
  const [myEventRegistrations, setMyEventRegistrations] = useState<EventRegistrationItem[]>([]);
  const [managedRegistrations, setManagedRegistrations] = useState<EventRegistrationItem[]>([]);
  const [managedEventId, setManagedEventId] = useState('');
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
  const canReviewApprovals = hasAnyRole(currentUser?.roles ?? [], ['ADMIN', 'RESOURCE_MANAGER']);

  useEffect(() => {
    if (authenticated) {
      void loadWorkspace();
    }
  }, [authenticated]);

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

      const nextManagedEventId = ownedEvents.some((item) => item.eventId === managedEventId)
        ? managedEventId
        : (ownedEvents[0]?.eventId ?? '');
      setManagedEventId(nextManagedEventId);
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

  async function submitAuth() {
    await withFeedback(async () => {
      const response = authMode === 'login' ? await login(email, password) : await register(email, password);
      writeAuthSession(response);
      setCurrentUser(response.user);
      setPassword('');
      navigate('/dashboard', { replace: true });
    }, authMode === 'login' ? 'Signed in successfully.' : 'Account created successfully.');
  }

  function startGoogleLogin() {
    window.location.assign(googleAuthorizeUrl());
  }

  async function completeOAuthCallback(search: string) {
    setOauthHandling(true);
    setError(null);
    setMessage(null);
    try {
      const params = new URLSearchParams(search);
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
      navigate('/auth/login', { replace: true });
    } finally {
      setOauthHandling(false);
    }
  }

  async function submitProfile() {
    await withFeedback(async () => {
      const response = await updateProfile(profileDraft.displayName, profileDraft.timezone);
      setProfile(response);
    }, 'Profile updated.');
  }

  async function submitPreferences() {
    await withFeedback(async () => {
      const response = await updateNotificationPreferences(preferences);
      setPreferences(response);
    }, 'Notification preferences updated.');
  }

  async function createEventAction() {
    await withFeedback(async () => {
      await createEvent(eventDraft);
      setEventDraft(defaultEventDraft);
      await loadWorkspace();
    }, 'Event created.');
  }

  async function createBookingAction() {
    await withFeedback(async () => {
      await createBooking(bookingDraft);
      setBookingDraft(defaultBookingDraft);
      await loadWorkspace();
    }, 'Booking created.');
  }

  async function publishEventAction(eventId: string) {
    await withFeedback(async () => {
      await publishEvent(eventId);
      await loadWorkspace();
    }, 'Event published.');
  }

  async function cancelEventAction(eventId: string) {
    await withFeedback(async () => {
      await cancelEvent(eventId);
      await loadWorkspace();
    }, 'Event cancelled.');
  }

  async function registerForEventAction(eventId: string) {
    await withFeedback(async () => {
      await registerForEvent(eventId);
      await loadWorkspace();
    }, 'Event registration created.');
  }

  async function cancelEventRegistrationAction(eventId: string) {
    await withFeedback(async () => {
      await cancelEventRegistration(eventId);
      await loadWorkspace();
    }, 'Event registration cancelled.');
  }

  async function changeManagedEvent(eventId: string) {
    setManagedEventId(eventId);
    await loadManagedRegistrations(eventId);
  }

  async function checkInRegistrationAction(eventId: string, registrationId: string) {
    await withFeedback(async () => {
      await checkInEventRegistration(eventId, registrationId);
      await loadManagedRegistrations(eventId);
      await loadWorkspace();
    }, 'Attendee checked in.');
  }

  async function cancelBookingAction(bookingId: string) {
    await withFeedback(async () => {
      await cancelBooking(bookingId);
      await loadWorkspace();
    }, 'Booking cancelled.');
  }

  async function markNotificationReadAction(notificationId: string) {
    await withFeedback(async () => {
      await markNotificationRead(notificationId);
      await loadWorkspace();
    }, 'Notification marked as read.');
  }

  async function approveAction(approvalId: string) {
    await withFeedback(async () => {
      await approveApproval(approvalId, 'Approved from frontend console');
      await loadWorkspace();
    }, 'Approval completed.');
  }

  async function rejectAction(approvalId: string) {
    await withFeedback(async () => {
      await rejectApproval(approvalId, 'Rejected from frontend console');
      await loadWorkspace();
    }, 'Approval rejected.');
  }

  function seedBookingResource(resourceId: string) {
    setBookingDraft((current) => ({ ...current, resourceId }));
  }

  function logout() {
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
    setManagedEventId('');
    setMessage('Signed out.');
    setError(null);
    navigate('/auth/login', { replace: true });
  }

  function handleRuntimeError(value: unknown, fallback: string) {
    if (value instanceof ApiError && value.status === 401) {
      logout();
      return;
    }
    setError(readError(value, fallback));
  }

  const initials = useMemo(() => {
    const source = profile?.displayName || currentUser?.email || 'TR';
    return source.slice(0, 2).toUpperCase();
  }, [currentUser?.email, profile?.displayName]);

  const rolesLabel = currentUser?.roles.join(', ') ?? 'Guest';

  const navigationItems = navItems
    .map((item) => item.path === '/notifications' ? { ...item, badge: unreadCount } : item)
    .filter((item) => !item.roles || hasAnyRole(currentUser?.roles ?? [], item.roles));

  const value: AppContextValue = {
    authMode,
    email,
    password,
    currentUser,
    profile,
    preferences,
    profileDraft,
    publishedEvents,
    myEvents,
    myEventRegistrations,
    managedRegistrations,
    managedEventId,
    resources,
    bookings,
    notifications,
    approvals,
    dashboardOverview,
    popularResources,
    unreadCount,
    eventDraft,
    bookingDraft,
    loading,
    message,
    error,
    oauthHandling,
    googleEnabled,
    authenticated,
    canReviewApprovals,
    initials,
    rolesLabel,
    navigationItems,
    setAuthMode,
    setEmail,
    setPassword,
    setProfileDraft,
    setPreferences,
    setEventDraft,
    setBookingDraft,
    submitAuth,
    startGoogleLogin,
    completeOAuthCallback,
    submitProfile,
    submitPreferences,
    createEventAction,
    createBookingAction,
    reloadWorkspace: loadWorkspace,
    publishEventAction,
    cancelEventAction,
    registerForEventAction,
    cancelEventRegistrationAction,
    changeManagedEvent,
    checkInRegistrationAction,
    cancelBookingAction,
    markNotificationReadAction,
    approveAction,
    rejectAction,
    seedBookingResource,
    logout
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useAppContext() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useAppContext must be used within AppProvider');
  }
  return context;
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
