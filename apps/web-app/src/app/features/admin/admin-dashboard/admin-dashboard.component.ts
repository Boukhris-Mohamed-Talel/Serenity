import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../shared/models/user.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  users: UserResponse[] = [];
  loading = true;

  constructor(private readonly userService: UserService) {}

  ngOnInit(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  get totalUsers(): number { return this.users.length; }
  get activeUsers(): number { return this.users.filter(u => u.isActive).length; }
  get doctorCount(): number { return this.users.filter(u => u.role === 'DOCTOR').length; }
  get patientCount(): number { return this.users.filter(u => u.role === 'PATIENT').length; }
}
