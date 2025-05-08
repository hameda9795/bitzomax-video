export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  role: string;
  subscribed: boolean;
}

export interface CheckoutSession {
  sessionId: string;
  url: string;
}