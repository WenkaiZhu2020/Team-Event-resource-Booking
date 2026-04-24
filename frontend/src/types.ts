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
