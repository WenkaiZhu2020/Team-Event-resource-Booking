import type { AuthResponse, EventDraft, EventItem, NotificationPreference, UserProfile } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
const ACCESS_TOKEN_KEY = 'trms_access_token';
const USER_KEY = 'trms_user';

interface ApiResponse<T> {
  data: T;
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
      registrationOpenAt: payload.registrationOpenAt || null,
      registrationCloseAt: payload.registrationCloseAt || null
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
    throw new Error(message);
  }

  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as ApiResponse<T>).data;
  }

  return payload as T;
}
