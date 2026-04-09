import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { appointmentDateToYmd, normalizeTimeHhMm } from '../../shared/utils/appointment-scheduling.utils';
import {
  AppointmentNotification,
  AppointmentResponse,
  AppointmentUnreadCountResponse,
  CalendarBusySlot,
  CreateAppointmentDoctorRequest,
  CreateAppointmentPatientRequest,
  TeleconsultationResponse
} from '../../shared/models/appointment.model';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {

  private readonly API_URL = `${environment.apiUrl}/appointments`;

  constructor(private readonly http: HttpClient) {}

  patientRequest(body: CreateAppointmentPatientRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.API_URL}/patient-request`, body);
  }

  doctorSchedule(body: CreateAppointmentDoctorRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.API_URL}/doctor-schedule`, body);
  }

  confirm(id: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`${this.API_URL}/${id}/confirm`, {});
  }

  cancel(id: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`${this.API_URL}/${id}/cancel`, {});
  }

  complete(id: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`${this.API_URL}/${id}/complete`, {});
  }

  reschedule(
    id: number,
    body: { appointmentDate: string; timeSlot: string }
  ): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`${this.API_URL}/${id}/reschedule`, body);
  }

  /** Opens in browser or downloads — caller should revoke object URL after use. */
  downloadCalendarIcs(id: number): Observable<Blob> {
    return this.http.get(`${this.API_URL}/${id}/calendar.ics`, { responseType: 'blob' });
  }

  getGoogleCalendarLink(id: number): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.API_URL}/${id}/google-calendar-link`);
  }

  getMine(): Observable<AppointmentResponse[]> {
    const params = new HttpParams().set('scope', 'mine');
    return this.http.get<AppointmentResponse[]>(`${this.API_URL}/list`, { params });
  }

  getAll(): Observable<AppointmentResponse[]> {
    const params = new HttpParams().set('scope', 'all');
    return this.http.get<AppointmentResponse[]>(`${this.API_URL}/list`, { params });
  }

  getById(id: number): Observable<AppointmentResponse> {
    return this.http.get<AppointmentResponse>(`${this.API_URL}/${id}`);
  }

  getTeleconsultation(id: number): Observable<TeleconsultationResponse> {
    return this.http.get<TeleconsultationResponse>(`${this.API_URL}/${id}/teleconsultation`);
  }

  startTeleconsultation(id: number): Observable<TeleconsultationResponse> {
    return this.http.patch<TeleconsultationResponse>(`${this.API_URL}/${id}/teleconsultation/start`, {});
  }

  getAppointmentNotifications(): Observable<AppointmentNotification[]> {
    return this.http.get<AppointmentNotification[]>(`${this.API_URL}/notifications/me`);
  }

  getAppointmentNotificationsUnreadCount(): Observable<AppointmentUnreadCountResponse> {
    return this.http.get<AppointmentUnreadCountResponse>(`${this.API_URL}/notifications/me/unread-count`);
  }

  markAppointmentNotificationRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/notifications/me/${id}/read`, {});
  }

  markAllAppointmentNotificationsRead(): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/notifications/me/read-all`, {});
  }

  /**
   * Patient: pass doctorUserId. Doctor: pass optional patientUserId to include that patient's other visits.
   */
  getCalendarHints(
    from: string,
    to: string,
    opts: { doctorUserId?: number; patientUserId?: number; excludeAppointmentId?: number }
  ): Observable<CalendarBusySlot[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (opts.doctorUserId != null) {
      params = params.set('doctorUserId', String(opts.doctorUserId));
    }
    if (opts.patientUserId != null) {
      params = params.set('patientUserId', String(opts.patientUserId));
    }
    if (opts.excludeAppointmentId != null) {
      params = params.set('excludeAppointmentId', String(opts.excludeAppointmentId));
    }
    return this.http.get<unknown[]>(`${this.API_URL}/calendar-hints`, { params }).pipe(
      map((rows) => (Array.isArray(rows) ? rows : []).map((r) => AppointmentService.normalizeCalendarBusySlot(r)))
    );
  }

  private static normalizeCalendarBusySlot(raw: unknown): CalendarBusySlot {
    const o = raw as Record<string, unknown>;
    const ymd = appointmentDateToYmd(o['appointmentDate']) ?? '';
    const ts = normalizeTimeHhMm(String(o['timeSlot'] ?? ''));
    const src = o['source'] === 'PATIENT' ? 'PATIENT' : 'DOCTOR';
    return { appointmentDate: ymd, timeSlot: ts, source: src };
  }
}
