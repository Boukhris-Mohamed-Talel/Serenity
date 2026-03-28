import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { CrisisAlertPayload } from '../../shared/models/mood.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CrisisAlertService {

  private readonly API_URL = 'http://localhost:8085/api/monitoring/alerts/stream';

  private eventSource: EventSource | null = null;
  private connectedDoctorId: number | null = null;
  private readonly seenAlertKeys = new Set<string>();

  private readonly alertsSubject = new BehaviorSubject<CrisisAlertPayload[]>([]);
  readonly alerts$: Observable<CrisisAlertPayload[]> = this.alertsSubject.asObservable();

  private readonly newAlertSubject = new Subject<CrisisAlertPayload>();
  readonly newAlert$: Observable<CrisisAlertPayload> = this.newAlertSubject.asObservable();

  constructor(
    private readonly authService: AuthService,
    private readonly ngZone: NgZone
  ) {
    this.authService.onLogout(() => this.disconnect());
  }

  connect(doctorId: number): void {
    if (!doctorId) {
      return;
    }

    if (this.eventSource && this.connectedDoctorId === doctorId) {
      return;
    }

    this.disconnect();

    const token = this.authService.getToken();
    const streamUrl = token
      ? `${this.API_URL}/${doctorId}?token=${encodeURIComponent(token)}`
      : `${this.API_URL}/${doctorId}`;

    this.eventSource = new EventSource(streamUrl);
    this.connectedDoctorId = doctorId;

    this.eventSource.onopen = () => {
      console.info('[SSE] Connected for doctorId=', doctorId);
    };

    this.eventSource.addEventListener('connected', () => {
      console.info('[SSE] Handshake event received for doctorId=', doctorId);
    });

    this.eventSource.addEventListener('crisis-alert', (event: Event) => {
      this.processIncomingEvent(event, doctorId, 'crisis-alert');
    });

    // Fallback for servers that emit unnamed SSE messages.
    this.eventSource.onmessage = (event: MessageEvent) => {
      this.processIncomingEvent(event, doctorId, 'message');
    };

    this.eventSource.onerror = () => {
      console.warn('[SSE] Connection error for doctorId=', doctorId);
    };
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.connectedDoctorId = null;
    this.seenAlertKeys.clear();
  }

  clearAlerts(): void {
    this.alertsSubject.next([]);
  }

  private processIncomingEvent(event: Event, doctorId: number, channel: 'crisis-alert' | 'message'): void {
    this.ngZone.run(() => {
      const data = (event as MessageEvent).data;
      const payload = this.parsePayload(data);
      if (!payload) {
        return;
      }
      if (this.connectedDoctorId !== null && payload.doctorId !== this.connectedDoctorId) {
        console.warn('[SSE] Ignoring alert for mismatched doctorId', payload.doctorId, 'expected', this.connectedDoctorId);
        return;
      }

      const key = this.buildAlertKey(payload);
      if (this.seenAlertKeys.has(key)) {
        return;
      }
      this.seenAlertKeys.add(key);

      const current = this.alertsSubject.value;
      this.alertsSubject.next([payload, ...current]);
      this.newAlertSubject.next(payload);
      console.info('[SSE] Crisis alert received via', channel, 'for doctorId=', doctorId, payload);
    });
  }

  private parsePayload(raw: unknown): CrisisAlertPayload | null {
    try {
      if (!raw) {
        return null;
      }

      if (typeof raw === 'string') {
        const parsed = JSON.parse(raw) as CrisisAlertPayload;
        return this.isAlertPayload(parsed) ? parsed : null;
      }

      if (typeof raw === 'object') {
        const payload = raw as CrisisAlertPayload;
        return this.isAlertPayload(payload) ? payload : null;
      }

      return null;
    } catch {
      console.warn('[SSE] Unable to parse alert payload:', raw);
      return null;
    }
  }

  private isAlertPayload(payload: CrisisAlertPayload | null | undefined): payload is CrisisAlertPayload {
    return !!payload
      && typeof payload.doctorId === 'number'
      && typeof payload.patientId === 'number'
      && typeof payload.moodLevel === 'number'
      && typeof payload.message === 'string';
  }

  private buildAlertKey(payload: CrisisAlertPayload): string {
    return [payload.doctorId, payload.patientId, payload.moodLevel, payload.timestamp].join('|');
  }
}

