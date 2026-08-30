export interface AuthUser {
  id: number;
  username: string;
  rol: string;
}

export interface LoginResponse {
  success: boolean;
  accessToken: string;
  usuario: AuthUser;
}

export interface RefreshTokenResponse {
  accessToken: string;
}
