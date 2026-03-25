import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  InsuranceClaimRequest,
  InsuranceClaimResponse,
  InsuranceNotification,
  NotificationUnreadCountResponse
} from '../../shared/models/insurance.model';
import { UserService } from './user.service';

@Injectable({
  providedIn: 'root'
})
export class InsuranceService {

  private readonly API_URL = `${environment.insuranceApiUrl}/insurance`;

  constructor(
    private readonly http: HttpClient,
    private readonly userService: UserService
  ) {}

  submitClaim(request: InsuranceClaimRequest, files: File[]): Observable<InsuranceClaimResponse> {
    const formData = new FormData();
    formData.append('description', request.description);
    formData.append('amount', request.amount.toString());
    formData.append('insuranceCompany', request.insuranceCompany);
    formData.append('insuranceGrade', request.insuranceGrade.toString());
    files.forEach(file => formData.append('files', file));
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims`, formData, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }

  getMyClaims(): Observable<InsuranceClaimResponse[]> {
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.get<InsuranceClaimResponse[]>(`${this.API_URL}/claims/me`, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }

  getAllClaims(): Observable<InsuranceClaimResponse[]> {
    return this.http.get<InsuranceClaimResponse[]>(`${this.API_URL}/claims`);
  }

  getClaimById(id: number): Observable<InsuranceClaimResponse> {
    return this.http.get<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}`);
  }

  approveClaim(id: number, montant: number): Observable<InsuranceClaimResponse> {
    return this.http.patch<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/approve?montant=${montant}`, {});
  }

  rejectClaim(id: number): Observable<InsuranceClaimResponse> {
    return this.http.patch<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/reject`, {});
  }

  deleteClaim(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/claims/${id}`);
  }

  getMyNotifications(): Observable<InsuranceNotification[]> {
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.get<InsuranceNotification[]>(`${this.API_URL}/notifications/me`, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }

  getUnreadNotificationsCount(): Observable<NotificationUnreadCountResponse> {
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.get<NotificationUnreadCountResponse>(`${this.API_URL}/notifications/me/unread-count`, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }

  markNotificationAsRead(notificationId: number): Observable<void> {
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.patch<void>(`${this.API_URL}/notifications/me/${notificationId}/read`, {}, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }

  markAllNotificationsAsRead(): Observable<void> {
    return this.userService.getCurrentUser().pipe(
      take(1),
      switchMap(user =>
        this.http.patch<void>(`${this.API_URL}/notifications/me/read-all`, {}, {
          headers: { 'X-User-Id': String(user.id) }
        })
      )
    );
  }
}
