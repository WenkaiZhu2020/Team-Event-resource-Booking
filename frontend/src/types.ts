export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface AuthUser {
  userId: string;
  email: string;
  roles: string[];
  status: string;
}

export interface AuthResponse {
  tokens: TokenResponse;
  user: AuthUser;
}

export interface UserProfile {
  userId: string;
  email: string;
  displayName: string;
  timezone: string;
  roleSummary: string;
  accountStatus: string;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationPreference {
  inAppEnabled: boolean;
  emailEnabled: boolean;
  reminderMinutesBefore: number;
}

export interface EventItem {
  eventId: string;
  organizerId: string;
  title: string;
  description: string | null;
  category: string;
  location: string;
  capacity: number;
  attendeeProjectedCount: number;
  waitlistProjectedCount: number;
  checkedInCount: number;
  registrationOpenAt: string | null;
  registrationCloseAt: string | null;
  startAt: string;
  endAt: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface EventDraft {
  title: string;
  description: string;
  category: string;
  location: string;
  capacity: number;
  registrationOpenAt: string;
  registrationCloseAt: string;
  startAt: string;
  endAt: string;
}

export interface EventRegistrationItem {
  registrationId: string;
  eventId: string;
  userId: string;
  status: string;
  waitlistPosition: number | null;
  registeredAt: string;
  cancelledAt: string | null;
  checkedInAt: string | null;
  checkedInBy: string | null;
  updatedAt: string;
}

export interface ResourceItem {
  resourceId: string;
  managerId: string;
  name: string;
  description: string | null;
  type: string;
  location: string;
  capacity: number | null;
  status: string;
  approvalMode: string;
  requiresApproval: boolean;
  allowWaitlist: boolean;
  maxBookingDurationMinutes: number;
  advanceBookingWindowDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface BookingItem {
  bookingId: string;
  userId: string;
  linkedEventId: string | null;
  resourceId: string;
  resourceName: string;
  resourceManagerId: string;
  resourceType: string;
  startAt: string;
  endAt: string;
  purpose: string;
  status: string;
  approvalMode: string;
  waitlistPosition: number | null;
  approvalRequestedAt: string | null;
  decidedAt: string | null;
  decisionNote: string | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookingDraft {
  resourceId: string;
  linkedEventId: string;
  startAt: string;
  endAt: string;
  purpose: string;
}

export interface NotificationItem {
  notificationId: string;
  userId: string;
  sourceEventId: string;
  sourceEventType: string;
  notificationType: string;
  channel: string;
  subject: string;
  body: string;
  status: string;
  readAt: string | null;
  sentAt: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UnreadCount {
  unreadCount: number;
}

export interface ApprovalHistoryItem {
  decisionId: string;
  action: string;
  actorId: string | null;
  note: string | null;
  actedAt: string;
  createdAt: string;
}

export interface ApprovalItem {
  approvalId: string;
  targetType: string;
  targetId: string;
  approvalType: string;
  requesterId: string;
  approverId: string;
  targetOwnerId: string | null;
  resourceId: string | null;
  title: string;
  summary: string | null;
  currentStep: number;
  totalSteps: number;
  status: string;
  submittedAt: string;
  decidedAt: string | null;
  decisionNote: string | null;
  createdAt: string;
  updatedAt: string;
  history: ApprovalHistoryItem[];
}

export interface DashboardOverview {
  totalBookings: number;
  approvedBookings: number;
  pendingApprovals: number;
  waitlistedBookings: number;
  cancelledOrRejectedBookings: number;
  uniqueResourcesUsed: number;
  nextSevenDaysApprovedBookings: number;
  totalApprovedReservedMinutes: number;
}

export interface ResourcePopularityItem {
  resourceId: string;
  resourceName: string;
  resourceType: string;
  totalBookings: number;
  approvedBookings: number;
  pendingBookings: number;
  waitlistedBookings: number;
  cancelledBookings: number;
  totalReservedMinutes: number;
  popularityScore: number;
  lastRefreshedAt: string;
}
