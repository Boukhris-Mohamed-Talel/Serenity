import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ProfileUpdateRequest, UserRequest, UserResponse } from '../../shared/models/user.model';
import { AuthService } from './auth.service';
import { UserName } from '../../shared/models/user-name.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly API_URL = `${environment.apiUrl}/users`;
  private cachedUser: UserResponse | null = null;
  private userRequest$: Observable<UserResponse> | null = null;
  private readonly currentUserSubject = new BehaviorSubject<UserResponse | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private readonly http: HttpClient, private readonly authService: AuthService) {
    this.authService.onLogout(() => this.clearCache());
  }

  getCurrentUser(): Observable<UserResponse> {
    if (this.cachedUser) {
      return of(this.cachedUser);
    }
    if (!this.userRequest$) {
      this.userRequest$ = this.http.get<UserResponse>(`${this.API_URL}/me`).pipe(
        tap(user => {
          this.cachedUser = user;
          this.currentUserSubject.next(user);
        }),
        shareReplay(1)
      );
    }
    return this.userRequest$;
  }

  refreshCurrentUser(): Observable<UserResponse> {
    this.cachedUser = null;
    this.userRequest$ = null;
    return this.getCurrentUser();
  }

  clearCache(): void {
    this.cachedUser = null;
    this.userRequest$ = null;
    this.currentUserSubject.next(null);
  }

  updateProfile(request: ProfileUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.API_URL}/me`, request).pipe(
      tap(user => {
        this.cachedUser = user;
        this.currentUserSubject.next(user);
      })
    );
  }

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.API_URL);
  }

  getUserById(id: number): Observable<UserResponse> {
    const token = this.authService.getToken();
    return this.http.get<UserResponse>(`${this.API_URL}/${id}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
  }

  updateUser(id: number, request: UserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.API_URL}/${id}`, request);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  deactivateUser(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/deactivate`, {});
  }

  activateUser(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/activate`, {});
  }

  getUsersNamesById(ids: number[]): Observable<UserName[]> {
    return this.http.get<UserName[]>(`${this.API_URL}/names`, {
      params: { ids: ids.join(',') }
    });
  }

}
