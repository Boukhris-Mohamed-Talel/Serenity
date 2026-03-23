import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { CrisisAlertService } from '../core/services/crisis-alert.service';
import { UserService } from '../core/services/user.service';
import { CrisisAlertPayload } from '../shared/models/mood.model';
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
  alerts: CrisisAlertPayload[] = [];
  notificationPanelOpen = false;
  private peekInterval: any;
  private userSub!: Subscription;
  private alertsSub!: Subscription;
  private authSub!: Subscription;

  constructor(
    public readonly authService: AuthService,
    private readonly crisisAlertService: CrisisAlertService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe();

      this.userSub = this.userService.currentUser$.subscribe(user => {
        this.user = user;
        if (user && !this.peekInterval) {
          this.startPeekAnimation();
        }
      });

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
    if (this.peekInterval) {
      clearInterval(this.peekInterval);
    }
    if (this.userSub) {
      this.userSub.unsubscribe();
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

  toggleNotificationPanel(): void {
    if (!this.authService.isDoctor()) {
      return;
    }
    this.notificationPanelOpen = !this.notificationPanelOpen;
  }

  clearAllAlerts(): void {
    this.crisisAlertService.clearAlerts();
    this.notificationPanelOpen = false;
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
}
