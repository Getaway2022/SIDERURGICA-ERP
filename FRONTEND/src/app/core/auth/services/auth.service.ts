import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthUser, LoginResponse, RefreshTokenResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  private readonly userKey = 'erp_user';
  private readonly tokenKey = 'erp_access_token';

  private isRefreshing$ = new BehaviorSubject<boolean>(false);

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(
        `${this.apiUrl}/login`,
        { username, password },
        {
          withCredentials: true,
        },
      )
      .pipe(
        tap((response) => {
          if (response.success && response.usuario) {
            localStorage.setItem(this.userKey, JSON.stringify(response.usuario));
            localStorage.setItem(this.tokenKey, response.accessToken);
          }
        }),
      );
  }

  logout(): void {
    localStorage.removeItem(this.userKey);
    localStorage.removeItem(this.tokenKey);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getUser(): AuthUser | null {
    const user = localStorage.getItem(this.userKey);
    if (!user) return null;

    try {
      return JSON.parse(user) as AuthUser;
    } catch {
      localStorage.removeItem(this.userKey);
      return null;
    }
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  getRol(): string | null {
    return this.getUser()?.rol ?? null;
  }

  refreshToken(): Observable<RefreshTokenResponse> {
    return this.http
      .post<RefreshTokenResponse>(
        `${this.apiUrl}/auth/refresh`,
        {},
        {
          withCredentials: true,
        },
      )
      .pipe(
        tap((response) => {
          if (response.accessToken) {
            this.setAccessToken(response.accessToken);
          }
        }),
      );
  }

  get refreshing$(): BehaviorSubject<boolean> {
    return this.isRefreshing$;
  }
}
