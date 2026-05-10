import type {
  ApprovalItem,
  AuthResponse,
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
  UnreadCount,
  UserProfile
} from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
const ACCESS_TOKEN_KEY = 'trms_access_token';
const USER_KEY = 'trms_user';

interface ApiResponse<T> {
  data: T;
}

interface PageResponse<T> {
  content: T[];
}

interface IdentifiedRecord {
  id?: string;
  bookingId?: string;
  notificationId?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly details?: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export function readAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function readStoredUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthResponse['user'];
  } catch {
    return null;
  }
}

export function writeAuthSession(session: AuthResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, session.tokens.accessToken);
  localStorage.setItem(USER_KEY, JSON.stringify(session.user));
}

export function clearAuthSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function googleAuthorizeUrl() {
  return `${API_BASE_URL}/v1/auth/oauth2/google/authorize`;
}

export async function register(email: string, password: string) {
  return request<AuthResponse>('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
}

export async function login(email: string, password: string) {
  return request<AuthResponse>('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
}

export async function getProfile() {
  return request<UserProfile>('/v1/users/me');
}

export async function updateProfile(displayName: string, timezone: string) {
  return request<UserProfile>('/v1/users/me', {
    method: 'PUT',
    body: JSON.stringify({ displayName, timezone })
  });
}

export async function getNotificationPreferences() {
  return request<NotificationPreference>('/v1/preferences/notifications');
}

export async function updateNotificationPreferences(payload: NotificationPreference) {
  return request<NotificationPreference>('/v1/preferences/notifications', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function getDashboardOverview() {
  return request<DashboardOverview>('/v1/analytics/dashboard/overview');
}

export async function getPopularResources() {
  return request<ResourcePopularityItem[]>('/v1/analytics/dashboard/resources/popular');
}

export async function getEvents() {
  return request<EventItem[]>('/v1/events');
}

export async function getMyEvents() {
  return request<EventItem[]>('/v1/events/me');
}

export async function createEvent(payload: EventDraft) {
  return request<EventItem>('/v1/events', {
    method: 'POST',
    body: JSON.stringify({
      ...payload,
      capacity: Number(payload.capacity),
      registrationOpenAt: toApiDateTime(payload.registrationOpenAt),
      registrationCloseAt: toApiDateTime(payload.registrationCloseAt),
      startAt: toApiDateTime(payload.startAt),
      endAt: toApiDateTime(payload.endAt)
    })
  });
}

export async function publishEvent(eventId: string) {
  return request<EventItem>(`/v1/events/${eventId}/publish`, {
    method: 'POST'
  });
}

export async function cancelEvent(eventId: string) {
  return request<EventItem>(`/v1/events/${eventId}/cancel`, {
    method: 'POST'
  });
}

export async function getMyEventRegistrations() {
  return request<EventRegistrationItem[]>('/v1/events/registrations/me');
}

export async function getEventRegistrations(eventId: string) {
  return request<EventRegistrationItem[]>(`/v1/events/${eventId}/registrations`);
}

export async function registerForEvent(eventId: string) {
  return request<EventRegistrationItem>(`/v1/events/${eventId}/registrations`, {
    method: 'POST'
  });
}

export async function cancelEventRegistration(eventId: string) {
  return request<EventRegistrationItem>(`/v1/events/${eventId}/registrations/cancel`, {
    method: 'POST'
  });
}

export async function checkInEventRegistration(eventId: string, registrationId: string) {
  return request<EventRegistrationItem>(`/v1/events/${eventId}/registrations/${registrationId}/check-in`, {
    method: 'POST'
  });
}

export async function getResources() {
  const resources = await request<ResourceItem[] | PageResponse<ResourceItem>>('/v1/resources');
  return Array.isArray(resources) ? resources : resources.content;
}

export async function getMyBookings() {
  const bookings = await request<Array<BookingItem & { id?: string }>>('/v1/bookings/me');
  return bookings.map((booking) => normalizeIdField<BookingItem>(booking, 'bookingId'));
}

export async function createBooking(payload: BookingDraft) {
  const booking = await request<BookingItem & { id?: string }>('/v1/bookings', {
    method: 'POST',
    body: JSON.stringify({
      resourceId: payload.resourceId,
      linkedEventId: payload.linkedEventId || null,
      startAt: toApiDateTime(payload.startAt),
      endAt: toApiDateTime(payload.endAt),
      purpose: payload.purpose
    })
  });
  return normalizeIdField<BookingItem>(booking, 'bookingId');
}

export async function cancelBooking(bookingId: string) {
  const booking = await request<BookingItem & { id?: string }>(`/v1/bookings/${bookingId}/cancel`, {
    method: 'POST'
  });
  return normalizeIdField<BookingItem>(booking, 'bookingId');
}

export async function getNotifications() {
  const notifications = await request<Array<NotificationItem & { id?: string; title?: string }>>('/v1/notifications/me');
  return notifications.map((notification) => ({
    ...normalizeIdField<NotificationItem>(notification, 'notificationId'),
    subject: notification.subject ?? notification.title ?? ''
  }));
}

export async function getUnreadCount() {
  return request<UnreadCount>('/v1/notifications/me/unread-count');
}

export async function markNotificationRead(notificationId: string) {
  return request<NotificationItem>(`/v1/notifications/${notificationId}/read`, {
    method: 'POST'
  });
}

export async function getPendingApprovals() {
  return request<ApprovalItem[]>('/v1/workflows/approvals/pending');
}

export async function approveApproval(approvalId: string, note?: string) {
  return request<ApprovalItem>(`/v1/workflows/approvals/${approvalId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ note: note || null })
  });
}

export async function rejectApproval(approvalId: string, note?: string) {
  return request<ApprovalItem>(`/v1/workflows/approvals/${approvalId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ note: note || null })
  });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = readAccessToken();
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = payload?.message ?? `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, payload?.details);
  }

  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as ApiResponse<T>).data;
  }

  return payload as T;
}

function toApiDateTime(value: string) {
  if (!value) {
    return null;
  }
  return new Date(value).toISOString();
}

function normalizeIdField<T extends IdentifiedRecord>(item: T, idField: 'bookingId' | 'notificationId'): T {
  if (!item[idField] && item.id) {
    return {
      ...item,
      [idField]: item.id
    };
  }
  return item;
}
