import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { AuthService } from '../../../../core/services/auth.service';
import { MoodEntryResponse } from '../../../../shared/models/mood.model';

@Component({
  selector: 'app-mood-list',
  templateUrl: './mood-list.component.html',
  styleUrls: ['./mood-list.component.scss']
})
export class MoodListComponent implements OnInit {
  moodEntries: MoodEntryResponse[] = [];
  loading = true;
  errorMessage = '';
  emptyState = false;
  isDoctorView = false;

  // Mood score color mapping
  moodColors: { [key: number]: string } = {
    1: '#e74c3c',  // Red - very bad
    2: '#e67e22',  // Orange - bad
    3: '#f39c12',  // Yellow-orange - poor
    4: '#f1c40f',  // Yellow - below average
    5: '#f4d03f',  // Light yellow - average
    6: '#a3e048',  // Light green - good
    7: '#2ecc71',  // Green - very good
    8: '#1abc9c',  // Teal - excellent
    9: '#3498db',  // Blue - very excellent
    10: '#9b59b6'  // Purple - perfect
  };

  moodLabels: { [key: number]: string } = {
    1: 'Very Bad',
    2: 'Bad',
    3: 'Poor',
    4: 'Below Average',
    5: 'Average',
    6: 'Good',
    7: 'Very Good',
    8: 'Excellent',
    9: 'Very Excellent',
    10: 'Perfect'
  };

  constructor(
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadMoodEntries();
  }

  loadMoodEntries(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'User not logged in or userId not available';
      this.loading = false;
      console.error('Current user:', currentUser);
      return;
    }

    this.isDoctorView = this.authService.isDoctor();
    const request$ = this.isDoctorView
      ? this.monitoringService.getMoodEntriesForDoctor(currentUser.userId)
      : this.monitoringService.getMoodEntries(currentUser.userId);

    request$.subscribe({
      next: (entries) => {
        this.moodEntries = entries;
        this.emptyState = entries.length === 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading mood entries:', err);
        this.errorMessage = err.error?.message || err.message || 'Failed to load mood entries';
        this.loading = false;
      }
    });
  }

  getMoodColor(score: number): string {
    return this.moodColors[score] || '#95a5a6';
  }

  getMoodLabel(score: number): string {
    return this.moodLabels[score] || 'Unknown';
  }

  getMoodEmoji(score: number): string {
    if (score <= 2) return '😢';
    if (score <= 4) return '😕';
    if (score <= 6) return '😐';
    if (score <= 8) return '🙂';
    return '😄';
  }

  createNewEntry(): void {
    if (this.isDoctorView) {
      return;
    }
    this.router.navigate(['/monitoring/new']);
  }

  editEntry(id: number): void {
    if (this.isDoctorView) {
      return;
    }
    this.router.navigate(['/monitoring/edit', id]);
  }

  deleteEntry(id: number): void {
    if (this.isDoctorView) {
      return;
    }
    if (confirm('Are you sure you want to delete this mood entry?')) {
      this.monitoringService.deleteMoodEntry(id).subscribe({
        next: () => {
          this.moodEntries = this.moodEntries.filter(entry => entry.id !== id);
          this.emptyState = this.moodEntries.length === 0;
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to delete mood entry';
        }
      });
    }
  }

  formatDate(date: string): string {
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getHeaderTitle(): string {
    return this.isDoctorView ? 'Patient Mood Monitoring' : 'Mood Tracking';
  }

  getHeaderSubtitle(): string {
    return this.isDoctorView
      ? 'Track mood entries from patients assigned to you'
      : 'Monitor your emotional wellness over time';
  }

  getPatientDisplayName(entry: MoodEntryResponse): string {
    return entry.patientName?.trim() || 'Unknown Patient';
  }

  getDoctorDisplayName(entry: MoodEntryResponse): string {
    return entry.doctorName?.trim() || 'Not assigned yet';
  }
}
