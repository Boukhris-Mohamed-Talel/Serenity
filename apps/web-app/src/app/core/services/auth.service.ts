import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, UserRequest } from '../../shared/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private readonly currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  currentUser$ = this.currentUserSubject.asObservable();

  private readonly onLogoutCallbacks: (() => void)[] = [];

  constructor(private readonly http: HttpClient) {}

  register(request: UserRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/oauth2/google`, { token: idToken }).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  loginWithFacebook(accessToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/oauth2/facebook`, { token: accessToken }).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  onLogout(callback: () => void): void {
    this.onLogoutCallbacks.push(callback);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.onLogoutCallbacks.forEach(cb => cb());
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const userRole = this.currentUserSubject.value?.role;
    if (!userRole || !role) {
      return false;
    }

    const normalize = (value: string) => value.replace(/^ROLE_/i, '').trim().toUpperCase();
    return normalize(userRole) === normalize(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  getUserId(): number | null {
    const storedUserId = this.currentUserSubject.value?.userId;
    if (typeof storedUserId === 'number' && Number.isFinite(storedUserId)) {
      return storedUserId;
    }

    return this.extractUserIdFromToken();
  }

  getUserEmail(): string | null {
    return this.currentUserSubject.value?.email ?? null;
  }

  private storeAuth(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.accessToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUserSubject.next(response);
  }

  private extractUserIdFromToken(): number | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const parts = token.split('.');
      if (parts.length < 2) {
        return null;
      }

      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      const payload = JSON.parse(atob(padded));
      const possibleValues = [payload?.userId, payload?.id, payload?.sub];

      for (const value of possibleValues) {
        const parsed = Number(value);
        if (Number.isFinite(parsed) && parsed > 0) {
          return parsed;
        }
      }
    } catch {
      return null;
    }

    return null;
  }

  private getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem(this.USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }
}
