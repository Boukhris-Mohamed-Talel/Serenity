import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  EmotionalTriggerRequest,
  EmotionalTriggerResponse,
  MoodEntryResponse
} from '../../../../shared/models/mood.model';

@Component({
  selector: 'app-mood-list',
  templateUrl: './mood-list.component.html',
  styleUrls: ['./mood-list.component.scss']
})
export class MoodListComponent implements OnInit {
  moodEntries: MoodEntryResponse[] = [];
  triggerMap: Record<number, EmotionalTriggerResponse[]> = {};
  triggerPanelOpen: Record<number, boolean> = {};
  triggerLoading: Record<number, boolean> = {};
  triggerSaving: Record<number, boolean> = {};
  triggerError: Record<number, string> = {};
  triggerForm: Record<number, EmotionalTriggerRequest> = {};
  editTriggerId: Record<number, number | null> = {};
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

  readonly triggerTypes: string[] = [
    'WORK_STRESS',
    'SLEEP_DEPRIVATION',
    'FAMILY_CONFLICT',
    'SOCIAL_ISOLATION',
    'TRAUMA',
    'FINANCIAL_STRESS',
    'COGNITIVE_OVERLOAD',
    'OTHER'
  ];

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

  toggleTriggerPanel(moodEntryId: number): void {
    const isOpen = this.triggerPanelOpen[moodEntryId];
    this.triggerPanelOpen[moodEntryId] = !isOpen;

    if (!isOpen) {
      this.loadTriggers(moodEntryId);
      this.resetTriggerForm(moodEntryId);
    }
  }

  loadTriggers(moodEntryId: number): void {
    this.triggerLoading[moodEntryId] = true;
    this.triggerError[moodEntryId] = '';

    this.monitoringService.getTriggersByMoodEntryId(moodEntryId).subscribe({
      next: (triggers) => {
        this.triggerMap[moodEntryId] = triggers || [];
        this.triggerLoading[moodEntryId] = false;
      },
      error: (err) => {
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to load clinical triggers';
        this.triggerLoading[moodEntryId] = false;
      }
    });
  }

  submitTrigger(moodEntryId: number): void {
    const form = this.triggerForm[moodEntryId];
    if (!form || !form.triggerType || !form.description || !form.intensity) {
      this.triggerError[moodEntryId] = 'Please fill all trigger fields correctly.';
      return;
    }
    if (form.description.trim().length < 10) {
      this.triggerError[moodEntryId] = 'Description must be at least 10 characters.';
      return;
    }

    this.triggerSaving[moodEntryId] = true;
    this.triggerError[moodEntryId] = '';

    const request: EmotionalTriggerRequest = {
      moodEntryId,
      triggerType: form.triggerType,
      description: form.description.trim(),
      intensity: Number(form.intensity)
    };

    const currentEditId = this.editTriggerId[moodEntryId];
    const save$ = currentEditId
      ? this.monitoringService.updateTrigger(currentEditId, request)
      : this.monitoringService.createTrigger(moodEntryId, request);

    save$.subscribe({
      next: () => {
        this.triggerSaving[moodEntryId] = false;
        this.resetTriggerForm(moodEntryId);
        this.loadTriggers(moodEntryId);
      },
      error: (err) => {
        this.triggerSaving[moodEntryId] = false;
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to save clinical trigger';
      }
    });
  }

  startEditTrigger(moodEntryId: number, trigger: EmotionalTriggerResponse): void {
    this.editTriggerId[moodEntryId] = trigger.id;
    this.triggerForm[moodEntryId] = {
      moodEntryId,
      triggerType: trigger.triggerType,
      description: trigger.description,
      intensity: trigger.intensity
    };
  }

  cancelEditTrigger(moodEntryId: number): void {
    this.resetTriggerForm(moodEntryId);
  }

  deleteTrigger(moodEntryId: number, triggerId: number): void {
    if (!confirm('Delete this clinical trigger?')) return;

    this.monitoringService.deleteTrigger(triggerId).subscribe({
      next: () => this.loadTriggers(moodEntryId),
      error: (err) => {
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to delete trigger';
      }
    });
  }

  private resetTriggerForm(moodEntryId: number): void {
    this.editTriggerId[moodEntryId] = null;
    this.triggerForm[moodEntryId] = {
      moodEntryId,
      triggerType: '',
      description: '',
      intensity: 5
    };
  }
}
