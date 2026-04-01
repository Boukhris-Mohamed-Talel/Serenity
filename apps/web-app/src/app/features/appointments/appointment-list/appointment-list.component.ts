import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { formatUserLookupName, UserLookup } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';

@Component({
  selector: 'app-appointment-list',
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss']
})
export class AppointmentListComponent implements OnInit {
  appointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage = '';
  isAdmin = false;
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
        this.appointments = rows;
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
}
