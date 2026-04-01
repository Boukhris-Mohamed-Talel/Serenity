import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ProfileUpdateRequest, UserLookup, UserRequest, UserResponse } from '../../shared/models/user.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly API_URL = `${environment.apiUrl}/users`;
  private readonly LOOKUP_URL = `${environment.apiUrl}/users/lookup`;
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
    return this.http.get<UserResponse>(`${this.API_URL}/${id}`);
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

  lookupDoctors(): Observable<UserLookup[]> {
    return this.http.get<UserLookup[]>(`${this.LOOKUP_URL}/doctors`);
  }

  /**
   * Omit both names to load all active patients (doctor schedule dropdown).
   * With firstName and/or lastName (each at least 2 chars when used), filters the list.
   */
  lookupPatients(firstName?: string, lastName?: string): Observable<UserLookup[]> {
    let params = new HttpParams();
    if (firstName?.trim()) {
      params = params.set('firstName', firstName.trim());
    }
    if (lastName?.trim()) {
      params = params.set('lastName', lastName.trim());
    }
    return this.http.get<UserLookup[]>(`${this.LOOKUP_URL}/patients`, { params });
  }

  /** Batch resolve user ids → names (POST /api/users/lookup/names). Same auth as other /users calls. */
  lookupNamesByIds(ids: number[]): Observable<UserLookup[]> {
    const unique = [...new Set(ids.filter((id) => Number.isFinite(id)))];
    if (unique.length === 0) {
      return of([]);
    }
    return this.http.post<UserLookup[]>(`${this.LOOKUP_URL}/names`, { ids: unique });
  }
}
