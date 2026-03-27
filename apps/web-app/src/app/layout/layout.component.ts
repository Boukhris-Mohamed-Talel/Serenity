import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
import { InsuranceService } from '../core/services/insurance.service';
import { InsuranceNotification } from '../shared/models/insurance.model';
import { UserResponse } from '../shared/models/user.model';
import { DebugEvent } from '../core/debug/debug-event.model';
import { DebugSessionService } from '../core/debug/debug-session.service';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentYear = new Date().getFullYear();
  characterVisible = false;
  user: UserResponse | null = null;
  notifications: InsuranceNotification[] = [];
  unreadNotificationCount = 0;
  notificationsOpen = false;
  notificationsLoading = false;
  private readonly locallyReadNotificationIds = new Set<number>();
  private peekInterval: any;
  private userSub!: Subscription;
  private routerSub!: Subscription;
  private debugSub!: Subscription;
  private notificationRefreshInterval: any;
  debugOpen = false;
  debugFilter = 'ALL';
  debugEvents: DebugEvent[] = [];

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly insuranceService: InsuranceService,
    private readonly router: Router,
    public readonly debugSessionService: DebugSessionService
  ) {}

  ngOnInit(): void {
    this.setupRouterDebugTracking();
    this.setupDebugEventsSubscription();

    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe();

      this.userSub = this.userService.currentUser$.subscribe(user => {
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
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
    if (this.debugSub) {
      this.debugSub.unsubscribe();
    }
    if (this.notificationRefreshInterval) {
      clearInterval(this.notificationRefreshInterval);
    }
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) return 'Anonymous';
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  getCharacterEmoji(): string {
    if (this.user?.profile?.isAnonymous) return '🥷';
    switch (this.user?.role) {
      case 'ADMIN': return '🛡️';
      case 'DOCTOR': return '👨‍⚕️';
      case 'PHARMACIST': return '💊';
      default: return '🧑';
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
    this.debugSessionService.log('AUTH', 'Manual logout from navbar');
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = !this.notificationsOpen;
    this.debugSessionService.log('UI_ACTION', 'Notification panel toggled', {
      open: this.notificationsOpen
    });
    if (this.notificationsOpen) {
      this.loadNotifications();
    }
  }

  onNotificationClick(notification: InsuranceNotification, event: MouseEvent): void {
    event.stopPropagation();
    this.debugSessionService.log('UI_ACTION', 'Notification clicked', {
      notificationId: notification.id,
      claimId: notification.claimId,
      wasRead: notification.isRead
    });
    const wasUnread = !notification.isRead;

    if (wasUnread) {
      this.locallyReadNotificationIds.add(notification.id);
      notification.isRead = true;
      this.insuranceService.markNotificationAsRead(notification.id).subscribe({
        next: () => {
          this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
          this.refreshNotifications();
        }
      });
    }

    this.notificationsOpen = false;
    if (notification.claimId != null) {
      this.router.navigate(['/insurance', notification.claimId]);
    }
  }

  markAllAsRead(event: MouseEvent): void {
    event.stopPropagation();
    this.debugSessionService.log('UI_ACTION', 'Mark all notifications as read');
    for (const notification of this.notifications) {
      this.locallyReadNotificationIds.add(notification.id);
    }
    this.insuranceService.markAllNotificationsAsRead().subscribe({
      next: () => {
        this.notifications = this.notifications.map(n => ({ ...n, isRead: true }));
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
  onDocumentClick(event: MouseEvent): void {
    this.notificationsOpen = false;
    const target = event.target as HTMLElement | null;
    if (target) {
      this.debugSessionService.log('UI_ACTION', 'Document click', {
        tag: target.tagName,
        id: target.id || null,
        className: target.className || null
      });
    }
  }

  @HostListener('document:keydown.control.shift.d')
  onDebugShortcut(): void {
    if (!this.debugSessionService.isEnabled()) {
      this.debugSessionService.enable();
    }
    this.debugOpen = !this.debugOpen;
  }

  get filteredDebugEvents(): DebugEvent[] {
    if (this.debugFilter === 'ALL') {
      return [...this.debugEvents].reverse();
    }
    return this.debugEvents
      .filter(event => event.category === this.debugFilter)
      .reverse();
  }

  toggleDebugPanel(): void {
    if (!this.debugSessionService.isEnabled()) {
      this.debugSessionService.enable();
    }
    this.debugOpen = !this.debugOpen;
  }

  clearDebugEvents(): void {
    this.debugSessionService.clear();
  }

  copyDebugJson(): void {
    const payload = this.debugSessionService.exportJson();
    navigator.clipboard.writeText(payload);
  }

  copyDebugMarkdown(): void {
    const payload = this.debugSessionService.exportMarkdown();
    navigator.clipboard.writeText(payload);
  }

  private setupRouterDebugTracking(): void {
    this.routerSub = this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.debugSessionService.log('NAVIGATION', 'Navigation start', {
          url: event.url
        });
      } else if (event instanceof NavigationEnd) {
        this.debugSessionService.log('NAVIGATION', 'Navigation end', {
          urlAfterRedirects: event.urlAfterRedirects
        });
        this.debugSessionService.checkMarketplaceRoute(event.urlAfterRedirects);
      } else if (event instanceof NavigationCancel) {
        this.debugSessionService.log('ERROR', 'Navigation canceled', {
          url: event.url,
          reason: event.reason
        }, 'warn');
      } else if (event instanceof NavigationError) {
        this.debugSessionService.log('ERROR', 'Navigation error', {
          url: event.url,
          message: event.error?.message || String(event.error)
        }, 'error');
      }
    });
  }

  private setupDebugEventsSubscription(): void {
    this.debugSub = this.debugSessionService.events$.subscribe(events => {
      this.debugEvents = events;
    });
  }

  private refreshNotifications(): void {
    this.insuranceService.getUnreadNotificationsCount().subscribe({
      next: (res) => {
        this.unreadNotificationCount = res.unreadCount || 0;
      }
    });
  }

  private loadNotifications(): void {
    this.notificationsLoading = true;
    this.insuranceService.getMyNotifications().subscribe({
      next: (items) => {
        this.notifications = (items || []).map(item => ({
          ...item,
          isRead: item.isRead || this.locallyReadNotificationIds.has(item.id)
        }));
        this.notificationsLoading = false;
      },
      error: () => {
        this.notificationsLoading = false;
      }
    });
  }
}
