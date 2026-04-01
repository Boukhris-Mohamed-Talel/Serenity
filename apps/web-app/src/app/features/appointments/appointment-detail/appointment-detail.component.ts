import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { UserLookup, formatUserLookupName } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  TeleconsultationResponse,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';

@Component({
  selector: 'app-appointment-detail',
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss']
})
export class AppointmentDetailComponent implements OnInit {
  appointment: AppointmentResponse | null = null;
  tele: TeleconsultationResponse | null = null;
  loading = true;
  errorMessage = '';
  /** Filled via POST /users/lookup/names (browser + gateway); avoids broken server-to-user-service calls. */
  private readonly nameByUserId = new Map<number, UserLookup>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.back();
      return;
    }
    this.appointmentService.getById(id).subscribe({
      next: (a) => {
        this.appointment = a;
        this.tele = a.teleconsultation;
        this.loading = false;
        this.refreshParticipantNames(a);
        if (a.type === 'TELECONSULTATION' && !this.tele) {
          this.refreshTele(id);
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Not found';
        this.loading = false;
      }
    });
  }

  private refreshParticipantNames(a: AppointmentResponse): void {
    this.userService.lookupNamesByIds([a.patientUserId, a.doctorUserId]).subscribe({
      next: (rows) => {
        this.nameByUserId.clear();
        for (const r of rows) {
          this.nameByUserId.set(r.id, r);
        }
      },
      error: () => {
        /* keep server-provided names or dashes */
      }
    });
  }

  private refreshTele(appointmentId: number): void {
    this.appointmentService.getTeleconsultation(appointmentId).subscribe({
      next: (t) => {
        this.tele = t;
      },
      error: () => {
        /* no tele yet */
      }
    });
  }

  get patientDisplayName(): string {
    if (!this.appointment) {
      return '—';
    }
    const row = this.nameByUserId.get(this.appointment.patientUserId);
    if (row) {
      return formatUserLookupName(row);
    }
    return appointmentParticipantDisplayName(this.appointment, 'patient');
  }

  get doctorDisplayName(): string {
    if (!this.appointment) {
      return '—';
    }
    const row = this.nameByUserId.get(this.appointment.doctorUserId);
    if (row) {
      return formatUserLookupName(row);
    }
    return appointmentParticipantDisplayName(this.appointment, 'doctor');
  }

  get isDoctor(): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && this.appointment != null && this.appointment.doctorUserId === uid;
  }

  get isPatient(): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && this.appointment != null && this.appointment.patientUserId === uid;
  }

  startVideo(): void {
    if (!this.appointment || this.appointment.status === 'COMPLETED') {
      return;
    }
    this.appointmentService.startTeleconsultation(this.appointment.id).subscribe({
      next: (t) => {
        this.tele = t;
        if (t.meetingUrl) {
          window.open(t.meetingUrl, '_blank', 'noopener,noreferrer');
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Could not start teleconsultation';
      }
    });
  }

  openRoom(): void {
    if (this.appointment?.status === 'COMPLETED' || !this.tele?.meetingUrl) {
      return;
    }
    window.open(this.tele.meetingUrl, '_blank', 'noopener,noreferrer');
  }

  complete(): void {
    if (!this.appointment) {
      return;
    }
    this.appointmentService.complete(this.appointment.id).subscribe({
      next: (a) => {
        this.appointment = a;
        this.tele = a.teleconsultation;
        this.refreshParticipantNames(a);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Could not complete';
      }
    });
  }

  back(): void {
    const dest = this.router.url.split('?')[0].includes('/admin/appointments')
      ? '/admin/appointments'
      : '/appointments';
    this.router.navigateByUrl(dest);
  }
}
