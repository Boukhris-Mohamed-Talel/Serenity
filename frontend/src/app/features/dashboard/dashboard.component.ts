import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../shared/models/user.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private user: UserResponse | null = null;

  constructor(
    public authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => this.user = user
    });
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) return 'Anonymous';
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }
}
