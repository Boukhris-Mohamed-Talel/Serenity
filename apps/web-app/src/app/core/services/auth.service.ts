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
    const user = this.currentUserSubject.value;
    const actualRole = this.normalizeRole(user?.role);
    const requiredRole = this.normalizeRole(role);
    if (!actualRole || !requiredRole) {
      return false;
    }
    return actualRole === requiredRole;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isPatient(): boolean {
    return this.hasRole('PATIENT');
  }

  isDoctor(): boolean {
    return this.hasRole('DOCTOR');
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  private storeAuth(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.accessToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUserSubject.next(response);
  }

  private getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem(this.USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  private normalizeRole(role: string | null | undefined): string {
    const value = role?.trim().toUpperCase() ?? '';
    return value.startsWith('ROLE_') ? value.substring(5) : value;
  }
}
