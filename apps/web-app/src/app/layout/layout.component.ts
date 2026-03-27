import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
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
  private peekInterval: any;
  private userSub!: Subscription;
  private wsSub!: Subscription;

  notifications: any[] = [];
  unreadCount = 0;
  notifDropdownVisible = false;

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly webSocketService: WebSocketService
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

        this.notifications.unshift({
          id: msg.id,
          text: msg.content,
          senderName,           // 👈
          conversationId: msg.conversationId,
          time: new Date(),
          read: false
        });
        this.unreadCount++;
      },
      error: () => {
        this.notifications.unshift({
          id: msg.id,
          text: msg.content,
          senderName: 'Unknown', // 👈 fallback
          conversationId: msg.conversationId,
          time: new Date(),
          read: false
        });
        this.unreadCount++;
      }
    });
  }
});
    }
  }

  ngOnDestroy(): void {
    if (this.peekInterval) clearInterval(this.peekInterval);
    if (this.userSub) this.userSub.unsubscribe();
    if (this.wsSub) this.wsSub.unsubscribe();
  }

  toggleNotifDropdown() {
    this.notifDropdownVisible = !this.notifDropdownVisible;
    if (this.notifDropdownVisible) {
      this.unreadCount = 0;
      this.notifications = this.notifications.map(n => ({ ...n, read: true }));
    }
  }

  goToConversation(notif: any) {
    this.notifDropdownVisible = false;
    this.router.navigate(['/messagerie']);
  }

  clearNotifications() {
    this.notifications = [];
    this.unreadCount = 0;
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
    this.router.navigate(['/auth/login']);
  }
}