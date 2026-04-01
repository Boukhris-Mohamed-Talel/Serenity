import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
import { AppointmentService } from '../core/services/appointment.service';
import { AppointmentNotification, NavbarNotification } from '../shared/models/appointment.model';
import { UserResponse } from '../shared/models/user.model';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentYear = new Date().getFullYear();
  characterVisible = false;
  user: UserResponse | null = null;
  notifications: NavbarNotification[] = [];
  unreadNotificationCount = 0;
  notificationsOpen = false;
  notificationsLoading = false;
  private readonly locallyReadNotificationIds = new Set<string>();
  private peekInterval: ReturnType<typeof setInterval> | undefined;
  private userSub!: Subscription;
  private notificationRefreshInterval: ReturnType<typeof setInterval> | undefined;

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly appointmentService: AppointmentService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe();

      this.userSub = this.userService.currentUser$.subscribe((user) => {
        this.user = user;
        if (user && !this.peekInterval) {
          this.startPeekAnimation();
        }
      });

      this.refreshNotifications();
      this.notificationRefreshInterval = setInterval(() => this.refreshNotifications(), 20000);
    }
  }

  ngOnDestroy(): void {
    if (this.peekInterval) {
      clearInterval(this.peekInterval);
    }
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
    if (this.notificationRefreshInterval) {
      clearInterval(this.notificationRefreshInterval);
    }
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) {
      return 'Anonymous';
    }
    if (this.user?.firstName) {
      return this.user.firstName;
    }
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  getCharacterEmoji(): string {
    if (this.user?.profile?.isAnonymous) {
      return '🥷';
    }
    switch (this.user?.role) {
      case 'ADMIN':
        return '🛡️';
      case 'DOCTOR':
        return '👨‍⚕️';
      default:
        return '🧑';
    }
  }

  private startPeekAnimation(): void {
    this.peekInterval = setInterval(() => {
      this.characterVisible = true;
      setTimeout(() => {
        this.characterVisible = false;
      }, 3000);
    }, 8000 + Math.random() * 7000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.loadNotifications();
    }
  }

  onNotificationClick(notification: NavbarNotification, event: MouseEvent): void {
    event.stopPropagation();
    const wasUnread = !notification.isRead;

    if (wasUnread) {
      this.locallyReadNotificationIds.add(`appointment-${notification.id}`);
      notification.isRead = true;
      this.appointmentService.markAppointmentNotificationRead(notification.id).subscribe({
        next: () => {
          this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
          this.refreshNotifications();
        }
      });
    }

    this.notificationsOpen = false;
    if (notification.appointmentId != null) {
      const base = this.router.url.split('?')[0].includes('/admin/')
        ? '/admin/appointments'
        : '/appointments';
      this.router.navigate([base, notification.appointmentId]);
    }
  }

  markAllAsRead(event: MouseEvent): void {
    event.stopPropagation();
    for (const notification of this.notifications) {
      this.locallyReadNotificationIds.add(`appointment-${notification.id}`);
    }
    this.appointmentService.markAllAppointmentNotificationsRead().pipe(catchError(() => of(undefined))).subscribe({
      next: () => {
        this.notifications = this.notifications.map((n) => ({ ...n, isRead: true }));
        this.unreadNotificationCount = 0;
        this.refreshNotifications();
      }
    });
  }

  formatNotificationDate(isoDate: string): string {
    const parsed = new Date(isoDate);
    return Number.isNaN(parsed.getTime()) ? '' : parsed.toLocaleString();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.notificationsOpen = false;
  }

  private refreshNotifications(): void {
    this.appointmentService.getAppointmentNotificationsUnreadCount().pipe(
      catchError(() => of({ unreadCount: 0 }))
    ).subscribe({
      next: (r) => {
        this.unreadNotificationCount = r.unreadCount || 0;
      }
    });
  }

  private loadNotifications(): void {
    this.notificationsLoading = true;
    this.appointmentService.getAppointmentNotifications().pipe(catchError(() => of([] as AppointmentNotification[]))).subscribe({
      next: (rows) => {
        const appointmentRows = rows || [];
        this.notifications = appointmentRows.map((n) => ({
          id: n.id,
          title: n.title,
          message: n.message,
          isRead: n.isRead || this.locallyReadNotificationIds.has(`appointment-${n.id}`),
          createdAt: this.normalizeNotificationDate(n.createdAt),
          appointmentId: n.appointmentId
        }));
        this.notifications.sort((a, b) => this.notificationSortKey(b.createdAt) - this.notificationSortKey(a.createdAt));
        this.notificationsLoading = false;
      },
      error: () => {
        this.notificationsLoading = false;
      }
    });
  }

  private normalizeNotificationDate(value: string | unknown): string {
    if (typeof value === 'string') {
      return value;
    }
    return new Date().toISOString();
  }

  private notificationSortKey(iso: string): number {
    const t = new Date(iso).getTime();
    return Number.isNaN(t) ? 0 : t;
  }
}
