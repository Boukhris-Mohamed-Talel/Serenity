import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { CrisisAlertService } from '../core/services/crisis-alert.service';
import { UserService } from '../core/services/user.service';
import { InsuranceService } from '../core/services/insurance.service';
import { InsuranceNotification } from '../shared/models/insurance.model';
import { CrisisAlertPayload } from '../shared/models/mood.model';
import { UserResponse } from '../shared/models/user.model';
import { WebSocketService } from '../core/services/web-socket.service';

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
  alerts: CrisisAlertPayload[] = [];
  notificationPanelOpen = false;
  private peekInterval: any;
  private userSub!: Subscription;
  private wsSub!: Subscription;
  
  // Message notifications from WebSocket
  messageNotifications: any[] = [];
  unreadMessageCount = 0;
  notifDropdownVisible = false;
  private alertsSub!: Subscription;
  private authSub!: Subscription;
  private notificationRefreshInterval: any;

  constructor(
    public readonly authService: AuthService,
    private readonly crisisAlertService: CrisisAlertService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly webSocketService: WebSocketService,
    private readonly insuranceService: InsuranceService
  ) {}

  ngOnInit(): void {
    document.addEventListener('click', () => {
      this.notifDropdownVisible = false;
    });
    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe();

      this.userSub = this.userService.currentUser$.subscribe(user => {
        this.user = user;
        if (user && !this.peekInterval) {
          this.startPeekAnimation();
        }
      });

      // 👇 WebSocket notifications
      this.webSocketService.connect();

      this.wsSub = this.webSocketService.newMessage$.subscribe((msg: any) => {
        const currentUserId = this.authService.getCurrentUser()?.userId;
        if (msg.senderId !== currentUserId && !msg.deletedMessageId) {
          // 👇 fetch sender name
          this.userService.getUsersNamesById([msg.senderId]).subscribe({
            next: (users) => {
              const sender = users[0];
              const senderName = sender ? `${sender.firstName} ${sender.lastName}` : 'Unknown';

              this.messageNotifications.unshift({
                id: msg.id,
                text: msg.content,
                senderName,           // 👈
                conversationId: msg.conversationId,
                time: new Date(),
                read: false
              });
              this.unreadMessageCount++;
            },
            error: () => {
              this.messageNotifications.unshift({
                id: msg.id,
                text: msg.content,
                senderName: 'Unknown', // 👈 fallback
                conversationId: msg.conversationId,
                time: new Date(),
                read: false
              });
              this.unreadMessageCount++;
            }
          });
        }
      });


      this.refreshNotifications();
      this.notificationRefreshInterval = setInterval(() => this.refreshNotifications(), 20000);

      this.alertsSub = this.crisisAlertService.alerts$.subscribe(alerts => {
        this.alerts = alerts;
      });
    }

    this.authSub = this.authService.currentUser$.subscribe((authUser) => {
      if (authUser && this.authService.isDoctor() && authUser.userId) {
        this.crisisAlertService.connect(authUser.userId);
        return;
      }
      this.crisisAlertService.disconnect();
    });
  }

  ngOnDestroy(): void {
    if (this.peekInterval) clearInterval(this.peekInterval);
    if (this.userSub) this.userSub.unsubscribe();
    if (this.wsSub) this.wsSub.unsubscribe();
  }

  toggleNotifDropdown() {
    this.notifDropdownVisible = !this.notifDropdownVisible;
    if (this.notifDropdownVisible) {
      this.unreadMessageCount = 0;
      this.messageNotifications = this.messageNotifications.map(n => ({ ...n, read: true }));
    }
  }

  goToConversation(notif: any) {
    this.notifDropdownVisible = false;
    this.router.navigate(['/messagerie']);
  }

  clearNotifications() {
    this.messageNotifications = [];
    this.unreadMessageCount = 0;
    if (this.peekInterval) {
      clearInterval(this.peekInterval);
    }
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
    if (this.notificationRefreshInterval) {
      clearInterval(this.notificationRefreshInterval);
    }
    if (this.alertsSub) {
      this.alertsSub.unsubscribe();
    }
    if (this.authSub) {
      this.authSub.unsubscribe();
    }
    if (this.authService.isDoctor()) {
      this.crisisAlertService.disconnect();
    }
  }

  get unreadCount(): number {
    return this.alerts.length + this.unreadMessageCount;
  }

  get showAlertPanel(): boolean {
    return this.notificationPanelOpen;
  }

  set showAlertPanel(value: boolean) {
    this.notificationPanelOpen = value;
  }

  toggleAlertPanel(): void {
    this.toggleNotificationPanel();
  }

  toggleNotificationPanel(): void {
    if (!this.authService.isDoctor()) {
      return;
    }
    this.notificationPanelOpen = !this.notificationPanelOpen;
  }

  clearAllAlerts(): void {
    this.crisisAlertService.clearAlerts();
    this.notificationPanelOpen = false;
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
    this.authService.logout();
    this.notificationPanelOpen = false;
    this.router.navigate(['/auth/login']);
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.loadNotifications();
    }
  }

  onNotificationClick(notification: InsuranceNotification, event: MouseEvent): void {
    event.stopPropagation();
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
  onDocumentClick(): void {
    this.notificationsOpen = false;
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