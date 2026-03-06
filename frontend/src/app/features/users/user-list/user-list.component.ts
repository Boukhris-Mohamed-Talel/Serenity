import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  users: UserResponse[] = [];
  loading = true;
  errorMessage = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
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
}
