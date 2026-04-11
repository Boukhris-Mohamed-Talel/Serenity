import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { BanDuration, UserResponse } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  users: UserResponse[] = [];
  loading = true;
  errorMessage = '';
  readonly banOptions: { label: string; value: BanDuration }[] = [
    { label: '1 day', value: 'ONE_DAY' },
    { label: '3 days', value: 'THREE_DAYS' },
    { label: '1 week', value: 'ONE_WEEK' },
    { label: '1 month', value: 'ONE_MONTH' },
    { label: 'Permanent', value: 'PERMANENT' }
  ];
  selectedBanDurationByUser: Record<number, BanDuration> = {};

  constructor(private readonly userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.selectedBanDurationByUser = users.reduce<Record<number, BanDuration>>((acc, user) => {
          acc[user.id] = 'ONE_DAY';
          return acc;
        }, {});
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load users';
        this.loading = false;
      }
    });
  }

  deactivateUser(id: number): void {
    if (!confirm('Are you sure you want to deactivate this user?')) return;

    this.userService.deactivateUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to deactivate user';
      }
    });
  }

  activateUser(id: number): void {
    this.userService.activateUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to activate user';
      }
    });
  }

  deleteUser(id: number): void {
    if (!confirm('Are you sure you want to permanently delete this user?')) return;

    this.userService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to delete user';
      }
    });
  }

  banUser(id: number): void {
    const duration = this.selectedBanDurationByUser[id] || 'ONE_DAY';
    const confirmMessage = duration === 'PERMANENT'
      ? 'Are you sure you want to permanently ban this user?'
      : 'Are you sure you want to ban this user?';
    if (!confirm(confirmMessage)) return;

    this.userService.banUser(id, duration).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to ban user';
      }
    });
  }

  unbanUser(id: number): void {
    this.userService.unbanUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to unban user';
      }
    });
  }

  isCurrentlyBanned(user: UserResponse): boolean {
    if (user.isPermanentlyBanned) {
      return true;
    }
    if (!user.bannedUntil) {
      return false;
    }
    return new Date(user.bannedUntil).getTime() > Date.now();
  }

  getBanStatusText(user: UserResponse): string {
    if (user.isPermanentlyBanned) {
      return 'Permanently banned';
    }
    if (this.isCurrentlyBanned(user) && user.bannedUntil) {
      return `Banned until ${new Date(user.bannedUntil).toLocaleString()}`;
    }
    return user.isActive ? 'Active' : 'Inactive';
  }
}
