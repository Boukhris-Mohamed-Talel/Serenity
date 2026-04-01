import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { formatUserLookupName, UserLookup } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  AppointmentStatus,
  AppointmentType,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';

export type AppointmentSortMode = 'date_time_asc' | 'date_time_desc';

@Component({
  selector: 'app-appointment-list',
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss']
})
export class AppointmentListComponent implements OnInit {
  /** Raw rows from API (before search / filters / sort). */
  allAppointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage = '';
  isAdmin = false;

  /** Search + filters + sort (patient / doctor / admin). */
  searchQuery = '';
  filterType: '' | AppointmentType = '';
  filterStatus: '' | AppointmentStatus = '';
  sortMode: AppointmentSortMode = 'date_time_desc';

  private readonly lookupById = new Map<number, UserLookup>();

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.load();
  }

  load(): void {
    this.loading = true;
    const req$ = this.isAdmin
      ? this.appointmentService.getAll()
      : this.appointmentService.getMine();
    req$.subscribe({
      next: (rows) => {
        this.allAppointments = rows;
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

  patientName(a: AppointmentResponse): string {
    const u = this.lookupById.get(a.patientUserId);
    if (u) {
      return formatUserLookupName(u);
    }
    return appointmentParticipantDisplayName(a, 'patient');
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
      case 'CONFIRMED': return 'badge badge-success';
      case 'CANCELLED': return 'badge badge-danger';
      case 'COMPLETED': return 'badge badge-muted';
      default: return 'badge badge-primary';
    }
  }

  isDoctorFor(a: AppointmentResponse): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && a.doctorUserId === uid;
  }

  isPatientFor(a: AppointmentResponse): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && a.patientUserId === uid;
  }

  confirm(a: AppointmentResponse, ev: Event): void {
    ev.stopPropagation();
    this.appointmentService.confirm(a.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = err.error?.error || 'Confirm failed';
      }
    });
  }

  cancel(a: AppointmentResponse, ev: Event): void {
    ev.stopPropagation();
    this.appointmentService.cancel(a.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = err.error?.error || 'Cancel failed';
      }
    });
  }

  openRow(a: AppointmentResponse): void {
    const base = this.router.url.split('?')[0].startsWith('/admin') ? '/admin/appointments' : '/appointments';
    this.router.navigate([base, a.id]);
  }

  get showAppointmentFilters(): boolean {
    return (
      this.authService.hasRole('PATIENT') ||
      this.authService.hasRole('DOCTOR') ||
      this.isAdmin
    );
  }

  /** Filtered + sorted rows when toolbar is shown. */
  get visibleAppointments(): AppointmentResponse[] {
    let rows = [...this.allAppointments];
    if (this.filterType) {
      rows = rows.filter((a) => a.type === this.filterType);
    }
    if (this.filterStatus) {
      rows = rows.filter((a) => a.status === this.filterStatus);
    }
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      rows = rows.filter((a) => this.rowMatchesQuery(a, q));
    }
    const mult = this.sortMode === 'date_time_desc' ? -1 : 1;
    rows.sort((a, b) => mult * (this.appointmentSortKey(a) - this.appointmentSortKey(b)));
    return rows;
  }

  /** Table source: filtered list when toolbar applies. */
  get displayAppointments(): AppointmentResponse[] {
    return this.showAppointmentFilters ? this.visibleAppointments : this.allAppointments;
  }

  get hasNoMatches(): boolean {
    return (
      !this.loading &&
      this.showAppointmentFilters &&
      this.allAppointments.length > 0 &&
      this.visibleAppointments.length === 0
    );
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.filterType = '';
    this.filterStatus = '';
    this.sortMode = 'date_time_desc';
  }

  private rowMatchesQuery(a: AppointmentResponse, q: string): boolean {
    const parts = [
      this.patientName(a),
      this.doctorName(a),
      a.appointmentDate,
      a.timeSlot,
      a.status,
      a.type,
      a.type === 'TELECONSULTATION' ? 'video teleconsultation' : 'in person',
      a.notes ?? ''
    ];
    return parts.some((p) => String(p).toLowerCase().includes(q));
  }

  /** Timestamp ms for date + time slot (chronological sort). */
  private appointmentSortKey(a: AppointmentResponse): number {
    const day = this.normalizeDatePart(a.appointmentDate);
    const mins = this.parseTimeSlotToMinutes(a.timeSlot);
    const t = Date.parse(`${day}T00:00:00`);
    const base = Number.isNaN(t) ? 0 : t;
    return base + mins * 60_000;
  }

  private normalizeDatePart(raw: string): string {
    if (!raw) {
      return '1970-01-01';
    }
    const s = raw.trim();
    if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
      return s.slice(0, 10);
    }
    const d = new Date(s);
    if (!Number.isNaN(d.getTime())) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    }
    return '1970-01-01';
  }

  private parseTimeSlotToMinutes(slot: string): number {
    if (!slot) {
      return 0;
    }
    const s = slot.trim().toUpperCase();
    const ampm = s.includes('PM') ? 'PM' : s.includes('AM') ? 'AM' : '';
    const core = s.replace(/\s*(AM|PM)\s*/i, '').trim();
    const m24 = core.match(/^(\d{1,2}):(\d{2})$/);
    if (m24) {
      let h = parseInt(m24[1], 10);
      const mi = parseInt(m24[2], 10);
      if (ampm === 'PM' && h < 12) {
        h += 12;
      }
      if (ampm === 'AM' && h === 12) {
        h = 0;
      }
      return h * 60 + mi;
    }
    return 0;
  }
}
