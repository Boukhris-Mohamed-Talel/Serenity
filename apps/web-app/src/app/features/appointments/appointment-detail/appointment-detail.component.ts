import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { UserLookup, formatUserLookupName } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  CalendarBusySlot,
  TeleconsultationResponse,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';
import {
  APPOINTMENT_SLOT_DURATION_MINUTES,
  appointmentDateToYmd,
  hasAppointmentTimeOverlap,
  minDateInputYmd,
  validateAppointmentScheduling
} from '../../../shared/utils/appointment-scheduling.utils';

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

  /** Reschedule form (same rules as booking). */
  rescheduleDate = '';
  rescheduleTime = '';
  rescheduleBusy: CalendarBusySlot[] = [];
  rescheduleCalMonth = new Date().getMonth() + 1;
  rescheduleCalYear = new Date().getFullYear();
  loadingRescheduleCal = false;
  rescheduleSubmitting = false;
  calendarLoadError = '';
  /** Reschedule form is shown only after clicking “Reschedule”. */
  showReschedulePanel = false;

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
        this.seedRescheduleFields(a);
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

  get minDateStr(): string {
    return minDateInputYmd();
  }

  get canReschedule(): boolean {
    const a = this.appointment;
    if (!a || (a.status !== 'PENDING' && a.status !== 'CONFIRMED')) {
      return false;
    }
    return this.isPatient || this.isDoctor;
  }

  get schedulingWindowError(): string | null {
    return validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
  }

  /** Prefill date/time from the appointment (no calendar API until panel opens). */
  private seedRescheduleFields(a: AppointmentResponse): void {
    const ymd = appointmentDateToYmd(a.appointmentDate) ?? '';
    this.rescheduleDate = ymd.length >= 10 ? ymd.slice(0, 10) : '';
    this.rescheduleTime = a.timeSlot;
    if (this.rescheduleDate.length >= 10) {
      const y = +this.rescheduleDate.slice(0, 4);
      const m = +this.rescheduleDate.slice(5, 7);
      if (Number.isFinite(y) && m >= 1 && m <= 12) {
        this.rescheduleCalYear = y;
        this.rescheduleCalMonth = m;
      }
    }
  }

  openReschedulePanel(): void {
    if (!this.appointment || !this.canReschedule) {
      return;
    }
    this.showReschedulePanel = true;
    this.calendarLoadError = '';
    this.seedRescheduleFields(this.appointment);
    this.loadRescheduleHints();
  }

  closeReschedulePanel(): void {
    this.showReschedulePanel = false;
  }

  private pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  private monthRange(y: number, m: number): { from: string; to: string } {
    const from = new Date(y, m - 1, 1);
    const to = new Date(y, m, 0);
    const fmt = (d: Date): string =>
      `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`;
    return { from: fmt(from), to: fmt(to) };
  }

  loadRescheduleHints(): void {
    const a = this.appointment;
    if (!a) {
      return;
    }
    const { from, to } = this.monthRange(this.rescheduleCalYear, this.rescheduleCalMonth);
    this.loadingRescheduleCal = true;
    this.calendarLoadError = '';
    const opts: { doctorUserId?: number; patientUserId?: number; excludeAppointmentId?: number } = {
      excludeAppointmentId: a.id
    };
    if (this.authService.hasRole('PATIENT')) {
      opts.doctorUserId = a.doctorUserId;
    } else if (this.authService.hasRole('DOCTOR')) {
      opts.patientUserId = a.patientUserId;
    }
    this.appointmentService.getCalendarHints(from, to, opts).subscribe({
      next: (rows) => {
        this.rescheduleBusy = rows;
        this.loadingRescheduleCal = false;
      },
      error: (err) => {
        this.rescheduleBusy = [];
        this.loadingRescheduleCal = false;
        this.calendarLoadError =
          err?.error?.message || err?.message || 'Could not load busy times.';
      }
    });
  }

  onRescheduleDateChange(value: string): void {
    this.rescheduleDate = value;
    if (value && value.length >= 10) {
      const y = +value.slice(0, 4);
      const m = +value.slice(5, 7);
      if (y > 0 && m >= 1 && m <= 12) {
        if (this.rescheduleCalYear !== y || this.rescheduleCalMonth !== m) {
          this.rescheduleCalYear = y;
          this.rescheduleCalMonth = m;
        }
        this.loadRescheduleHints();
      }
    }
  }

  onRescheduleCalMonthChange(ev: { year: number; month: number }): void {
    this.rescheduleCalYear = ev.year;
    this.rescheduleCalMonth = ev.month;
    this.loadRescheduleHints();
  }

  hasRescheduleSlotConflict(): boolean {
    if (this.schedulingWindowError || this.rescheduleDate.length < 10) {
      return false;
    }
    return hasAppointmentTimeOverlap(
      this.rescheduleDate,
      this.rescheduleTime,
      this.rescheduleBusy,
      APPOINTMENT_SLOT_DURATION_MINUTES
    );
  }

  submitReschedule(): void {
    const a = this.appointment;
    if (!a || !this.canReschedule) {
      return;
    }
    const err = validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
    if (err) {
      this.errorMessage = err;
      return;
    }
    if (this.hasRescheduleSlotConflict()) {
      this.errorMessage = 'That time overlaps another visit. Pick a different slot.';
      return;
    }
    this.rescheduleSubmitting = true;
    this.errorMessage = '';
    const body = {
      appointmentDate: this.rescheduleDate.slice(0, 10),
      timeSlot: this.rescheduleTime.trim()
    };
    this.appointmentService.reschedule(a.id, body).subscribe({
      next: (updated) => {
        this.appointment = updated;
        this.tele = updated.teleconsultation;
        this.rescheduleSubmitting = false;
        this.seedRescheduleFields(updated);
        this.showReschedulePanel = false;
      },
      error: (err) => {
        this.rescheduleSubmitting = false;
        this.errorMessage =
          err.error?.message || err.error?.error || err.message || 'Failed to reschedule';
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
    if (!this.appointment || this.appointment.status === 'COMPLETED' || this.appointment.status === 'CANCELLED') {
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
    if (
      this.appointment?.status === 'COMPLETED' ||
      this.appointment?.status === 'CANCELLED' ||
      !this.tele?.meetingUrl
    ) {
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
    const qp = this.route.snapshot.queryParams;
    this.router.navigate([dest], {
      queryParams: Object.keys(qp).length ? qp : undefined
    });
  }
}
