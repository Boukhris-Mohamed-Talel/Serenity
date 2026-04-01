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
  readonly dailyFocus = {
    title: 'Daily mindfulness',
    today: "Today’s Task: Deep Breathing (10 min)",
    items: ['Guided Meditation'],
    notePlaceholder: 'Note...'
  };

  readonly vitals = {
    heartRateBpm: 72,
    sleepHours: 8.5
  };

  readonly careCircle: Array<{
    name: string;
    role: string;
    initials: string;
  }> = [
    { name: 'Dr. Elara Vance', role: 'Primary', initials: 'EV' },
    { name: 'Nurse Chen', role: 'Coordinator', initials: 'NC' }
  ];

  readonly upcomingAppointments: Array<{
    title: string;
    when: string;
  }> = [
    { title: 'First Session - Dr. X', when: 'April 2, 10:00 AM'},
    { title: 'Follow up - Dr. X', when: 'April 12, 7:00 AM'},
    { title: 'Next Checkup - Dr. X', when: 'April 22, 8:00 AM'}
  ];

  readonly currentMedications: Array<{ name: string; schedule: string }> = [
    { name: 'Xanax 5mg', schedule: 'Daily' },
    { name: 'Advil 10mg', schedule: 'Morning' },
    { name: 'Ibuprofen 10mg', schedule: 'Daily' },
    { name: 'Naproxen 10mg', schedule: 'Morning' }
  ];

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService
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

  onContactCareCircle(): void {
    // Hook up to messaging flow when available.
  }
}