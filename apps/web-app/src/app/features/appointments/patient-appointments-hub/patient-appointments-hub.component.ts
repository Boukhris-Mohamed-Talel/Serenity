import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { formatUserLookupName, UserLookup } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  CalendarBusySlot,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';
import {
  appointmentDateToYmd,
  appointmentResponseStartMs,
  formatCountdownMs,
  formatSlotRange,
  minDateInputYmd,
  normalizeTimeHhMm
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-patient-appointments-hub',
  templateUrl: './patient-appointments-hub.component.html',
  styleUrls: ['./patient-appointments-hub.component.scss']
})
export class PatientAppointmentsHubComponent implements OnInit, OnDestroy {
  appointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage = '';
  private readonly lookupById = new Map<number, UserLookup>();

  /** Calendar: all visits (any status) so the month reflects history + future. */
  calendarBusySlots: CalendarBusySlot[] = [];
  calMonth = new Date().getMonth() + 1;
  calYear = new Date().getFullYear();
  selectedDate = '';

  countdownText = '';
  userFirstName = '';

  private userSub?: Subscription;
  private tickId: ReturnType<typeof setInterval> | undefined;

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.selectedDate = minDateInputYmd();
    this.userSub = this.userService.currentUser$.subscribe((u) => {
      this.userFirstName = u?.firstName?.trim() || '';
    });
    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.userFirstName = u?.firstName?.trim() || '';
      },
      error: () => {
        /* keep greeting generic */
      }
    });
    this.load();
    this.tickId = setInterval(() => this.refreshCountdown(), 1000);
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    if (this.tickId) {
      clearInterval(this.tickId);
    }
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.appointmentService.getMine().subscribe({
      next: (rows) => {
        this.appointments = rows;
        this.rebuildCalendarSlots();
        this.refreshCountdown();
        const ids = [...new Set(rows.flatMap((a) => [a.patientUserId, a.doctorUserId]))];
        if (ids.length === 0) {
          this.loading = false;
          return;
        }
        this.userService.lookupNamesByIds(ids).subscribe({
          next: (list) => {
            this.lookupById.clear();
            for (const u of list) {
              this.lookupById.set(u.id, u);
            }
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error?.error || err.message || 'Failed to load appointments';
        this.loading = false;
      }
    });
  }

  private rebuildCalendarSlots(): void {
    this.calendarBusySlots = this.appointments.map((a) => ({
      appointmentDate: appointmentDateToYmd(a.appointmentDate as unknown) ?? String(a.appointmentDate).slice(0, 10),
      timeSlot: normalizeTimeHhMm(a.timeSlot),
      source: 'PATIENT' as const
    }));
  }

  get greeting(): string {
    return this.userFirstName ? `Hi, ${this.userFirstName}` : 'Hi there';
  }

  /** Next PENDING or CONFIRMED visit strictly in the future. */
  get nextUpcoming(): AppointmentResponse | null {
    const now = Date.now();
    const upcoming = this.appointments
      .filter((a) => a.status === 'PENDING' || a.status === 'CONFIRMED')
      .map((a) => ({ a, t: appointmentResponseStartMs(a) }))
      .filter((x) => x.t > now)
      .sort((x, y) => x.t - y.t);
    return upcoming.length ? upcoming[0].a : null;
  }

  /** Confirmed or pending visits in the future (for "coming up" stat). */
  get upcomingActiveCount(): number {
    const now = Date.now();
    return this.appointments.filter(
      (a) =>
        (a.status === 'PENDING' || a.status === 'CONFIRMED') && appointmentResponseStartMs(a) > now
    ).length;
  }

  get completedCount(): number {
    return this.appointments.filter((a) => a.status === 'COMPLETED').length;
  }

  get hasVisits(): boolean {
    return this.appointments.length > 0;
  }

  /** Short line for patients: pending vs confirmed matters clinically. */
  statusPatientHint(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Your doctor still needs to confirm this visit.';
      case 'CONFIRMED':
        return 'Confirmed — you are on the schedule.';
      default:
        return '';
    }
  }

  private refreshCountdown(): void {
    const next = this.nextUpcoming;
    if (!next) {
      this.countdownText = '';
      return;
    }
    const target = appointmentResponseStartMs(next);
    const diff = target - Date.now();
    this.countdownText = formatCountdownMs(diff);
  }

  doctorName(a: AppointmentResponse): string {
    const u = this.lookupById.get(a.doctorUserId);
    if (u) {
      return formatUserLookupName(u);
    }
    return appointmentParticipantDisplayName(a, 'doctor');
  }

  statusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'hub-badge hub-badge--confirmed';
      case 'CANCELLED':
        return 'hub-badge hub-badge--cancelled';
      case 'COMPLETED':
        return 'hub-badge hub-badge--completed';
      default:
        return 'hub-badge hub-badge--pending';
    }
  }

  rangeLabel(timeSlot: string): string {
    return formatSlotRange(timeSlot);
  }

  get appointmentsOnSelectedDay(): AppointmentResponse[] {
    const key = this.selectedDate.slice(0, 10);
    return this.appointments
      .filter((a) => (appointmentDateToYmd(a.appointmentDate as unknown) ?? '').slice(0, 10) === key)
      .sort((a, b) => appointmentResponseStartMs(a) - appointmentResponseStartMs(b));
  }

  onCalMonthChange(ev: { year: number; month: number }): void {
    this.calYear = ev.year;
    this.calMonth = ev.month;
  }

  onDateChange(value: string): void {
    this.selectedDate = value;
    if (value && value.length >= 10) {
      const y = +value.slice(0, 4);
      const m = +value.slice(5, 7);
      if (y > 0 && m >= 1 && m <= 12) {
        if (this.calYear !== y || this.calMonth !== m) {
          this.calYear = y;
          this.calMonth = m;
        }
      }
    }
  }

  openAppointment(a: AppointmentResponse): void {
    this.router.navigate(['/appointments', a.id]);
  }

  onAppointmentItemKeydown(event: KeyboardEvent, a: AppointmentResponse): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.openAppointment(a);
    }
  }

  goBook(): void {
    this.router.navigate(['/appointments', 'book']);
  }

  goList(): void {
    this.router.navigate(['/appointments', 'list']);
  }
}
