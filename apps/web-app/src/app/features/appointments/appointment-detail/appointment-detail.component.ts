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
  appointmentDateToYmd,
  hasAppointmentTimeOverlap,
  minDateInputYmd,
  normalizeTimeHhMm,
  validateAppointmentScheduling
} from '../../../shared/utils/appointment-scheduling.utils';

function formatHttpError(err: unknown): string {
  const e = err as {
    error?: string | { error?: string; message?: string; path?: string };
    message?: string;
    status?: number;
  };
  if (typeof e?.error === 'string') {
    return e.error;
  }
  const body = e?.error as { error?: string; message?: string; path?: string } | undefined;
  if (body?.error) {
    return body.error;
  }
  if (body?.message) {
    return body.message;
  }
  return e?.message || 'Could not reschedule';
}

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

  /** Reschedule (patient or doctor): new date/time with same calendar/busy rules as booking. */
  reschedulePanelOpen = false;
  rescheduleDate = '';
  rescheduleTime = '';
  rescheduleBusySlots: CalendarBusySlot[] = [];
  rescheduleCalMonth = new Date().getMonth() + 1;
  rescheduleCalYear = new Date().getFullYear();
  rescheduleLoadingCalendar = false;
  rescheduleCalendarError = '';
  rescheduleSubmitting = false;
  rescheduleError = '';

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

  get canReschedule(): boolean {
    if (!this.appointment || (!this.isPatient && !this.isDoctor)) {
      return false;
    }
    const s = this.appointment.status;
    return s === 'PENDING' || s === 'CONFIRMED';
  }

  get rescheduleLegendDoctor(): string {
    return this.isPatient ? 'Doctor (already booked)' : 'Your appointments';
  }

  get rescheduleLegendPatient(): string {
    return this.isPatient ? 'Your other visits' : "Patient's other visits";
  }

  get rescheduleBusyPanelSubheading(): string {
    return this.isPatient
      ? 'Avoid the same start time (HH:mm) as these rows — your current booking is excluded.'
      : "Your bookings and this patient's other visits — current appointment excluded.";
  }

  /** Meeting URL must not be used after the visit is finished or the booking is cancelled. */
  get isTeleMeetingUnavailable(): boolean {
    const s = this.appointment?.status;
    return s === 'COMPLETED' || s === 'CANCELLED';
  }

  get minDateStr(): string {
    return minDateInputYmd();
  }

  openReschedulePanel(): void {
    if (!this.appointment) {
      return;
    }
    this.rescheduleError = '';
    const d = this.ymdFromAppointment(this.appointment);
    this.rescheduleDate = d;
    this.rescheduleTime = normalizeTimeHhMm(String(this.appointment.timeSlot ?? ''));
    const parts = d.split('-');
    if (parts.length === 3) {
      this.rescheduleCalYear = +parts[0];
      this.rescheduleCalMonth = +parts[1];
    }
    this.reschedulePanelOpen = true;
    this.loadRescheduleCalendarHints();
  }

  closeReschedulePanel(): void {
    this.reschedulePanelOpen = false;
    this.rescheduleError = '';
  }

  private ymdFromAppointment(a: AppointmentResponse): string {
    const raw = appointmentDateToYmd(a.appointmentDate as unknown);
    return raw ?? String(a.appointmentDate).slice(0, 10);
  }

  private pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  private rescheduleMonthRange(y: number, m: number): { from: string; to: string } {
    const from = new Date(y, m - 1, 1);
    const to = new Date(y, m, 0);
    const fmt = (d: Date): string =>
      `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`;
    return { from: fmt(from), to: fmt(to) };
  }

  loadRescheduleCalendarHints(): void {
    if (!this.appointment) {
      return;
    }
    const { from, to } = this.rescheduleMonthRange(this.rescheduleCalYear, this.rescheduleCalMonth);
    this.rescheduleLoadingCalendar = true;
    this.rescheduleCalendarError = '';
    const id = this.appointment.id;
    const opts: { doctorUserId?: number; patientUserId?: number; excludeAppointmentId: number } = {
      excludeAppointmentId: id
    };
    if (this.authService.hasRole('PATIENT')) {
      opts.doctorUserId = this.appointment.doctorUserId;
    }
    if (this.authService.hasRole('DOCTOR')) {
      opts.patientUserId = this.appointment.patientUserId;
    }
    this.appointmentService.getCalendarHints(from, to, opts).subscribe({
      next: (rows) => {
        this.rescheduleBusySlots = rows;
        this.rescheduleLoadingCalendar = false;
      },
      error: (err) => {
        this.rescheduleBusySlots = [];
        this.rescheduleLoadingCalendar = false;
        this.rescheduleCalendarError =
          err?.error?.message || err?.error?.error || err?.message || 'Could not load busy times.';
      }
    });
  }

  onRescheduleCalMonthChange(ev: { year: number; month: number }): void {
    this.rescheduleCalYear = ev.year;
    this.rescheduleCalMonth = ev.month;
    this.loadRescheduleCalendarHints();
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
        this.loadRescheduleCalendarHints();
      }
    }
  }

  get rescheduleSchedulingError(): string | null {
    if (!this.rescheduleDate || !this.rescheduleTime?.trim()) {
      return null;
    }
    return validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
  }

  hasRescheduleSlotConflict(): boolean {
    if (!this.rescheduleDate || !this.rescheduleTime?.trim()) {
      return false;
    }
    return hasAppointmentTimeOverlap(
      this.rescheduleDate,
      this.rescheduleTime,
      this.rescheduleBusySlots
    );
  }

  submitReschedule(): void {
    this.rescheduleError = '';
    if (!this.appointment) {
      return;
    }
    if (!this.rescheduleDate || !this.rescheduleTime?.trim()) {
      this.rescheduleError = 'Date and time are required.';
      return;
    }
    const schedErr = validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
    if (schedErr) {
      this.rescheduleError = schedErr;
      return;
    }
    if (this.hasRescheduleSlotConflict()) {
      this.rescheduleError =
        'That time overlaps an existing appointment (each visit blocks about 1h30).';
      return;
    }
    this.rescheduleSubmitting = true;
    const slot = normalizeTimeHhMm(this.rescheduleTime);
    const dateYmd =
      appointmentDateToYmd(this.rescheduleDate as unknown) ??
      String(this.rescheduleDate).slice(0, 10);
    this.appointmentService
      .reschedule(this.appointment.id, { appointmentDate: dateYmd, timeSlot: slot })
      .subscribe({
        next: (a) => {
          this.appointment = a;
          this.tele = a.teleconsultation;
          this.refreshParticipantNames(a);
          if (a.type === 'TELECONSULTATION' && !this.tele) {
            this.refreshTele(a.id);
          }
          this.reschedulePanelOpen = false;
          this.rescheduleSubmitting = false;
          this.errorMessage = '';
        },
        error: (err) => {
          this.rescheduleError = formatHttpError(err);
          this.rescheduleSubmitting = false;
        }
      });
  }

  startVideo(): void {
    if (!this.appointment || this.isTeleMeetingUnavailable) {
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
    if (this.isTeleMeetingUnavailable || !this.tele?.meetingUrl) {
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
