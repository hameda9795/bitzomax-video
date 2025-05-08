export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  profilePicture?: string;
  bio?: string;
  role: 'USER' | 'ADMIN';
  subscribed: boolean;
  subscriptionStartDate?: string;
  subscriptionEndDate?: string;
}

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  profilePicture?: string;
}

export interface UserProfile extends UserSummary {
  bio?: string;
  totalVideos?: number;
  totalLikes?: number;
  joinedDate?: string;
}

export interface ProfileUpdate {
  fullName?: string;
  bio?: string;
  profilePicture?: File;
}

export interface SubscriptionInfo {
  subscribed: boolean;
  subscriptionStartDate?: string;
  subscriptionEndDate?: string;
  stripeStatus?: string;
  currentPeriodEnd?: number;
}