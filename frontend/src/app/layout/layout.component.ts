import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { UserService } from '../core/services/user.service';
import { UserResponse } from '../shared/models/user.model';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentYear = new Date().getFullYear();
  characterVisible = false;
  private user: UserResponse | null = null;
  private peekInterval: any;

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe({
        next: (user) => {
          this.user = user;
          this.startPeekAnimation();
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.peekInterval) {
      clearInterval(this.peekInterval);
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
    this.router.navigate(['/auth/login']);
  }
}
