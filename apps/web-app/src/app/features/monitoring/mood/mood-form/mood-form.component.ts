import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { AuthService } from '../../../../core/services/auth.service';
import { MoodEntryResponse } from '../../../../shared/models/mood.model';

@Component({
  selector: 'app-mood-form',
  templateUrl: './mood-form.component.html',
  styleUrls: ['./mood-form.component.scss']
})
export class MoodFormComponent implements OnInit {
  moodForm!: FormGroup;
  isEditMode = false;
  editingId: number | null = null;
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  moodScales = [
    { value: 1, label: 'Very Bad 😢', color: '#e74c3c' },
    { value: 2, label: 'Bad 😞', color: '#e67e22' },
    { value: 3, label: 'Poor 😕', color: '#f39c12' },
    { value: 4, label: 'Below Average 😐', color: '#f1c40f' },
    { value: 5, label: 'Average 😐', color: '#f4d03f' },
    { value: 6, label: 'Good 🙂', color: '#a3e048' },
    { value: 7, label: 'Very Good 😊', color: '#2ecc71' },
    { value: 8, label: 'Excellent 😄', color: '#1abc9c' },
    { value: 9, label: 'Very Excellent 😃', color: '#3498db' },
    { value: 10, label: 'Perfect 😄', color: '#9b59b6' }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.checkEditMode();
  }

  private initializeForm(): void {
    this.moodForm = this.fb.group({
      moodScore: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
      moodDescription: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      triggers: ['', [Validators.maxLength(500)]]
    });
  }

  private checkEditMode(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.editingId = parseInt(id, 10);
        this.loadMoodEntry(this.editingId);
      }
    });
  }

  private loadMoodEntry(id: number): void {
    this.loading = true;
    this.monitoringService.getMoodEntryById(id).subscribe({
      next: (entry) => {
        this.moodForm.patchValue({
          moodScore: entry.moodScore,
          moodDescription: entry.moodDescription,
          triggers: entry.triggers || ''
        });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load mood entry';
        this.loading = false;
      }
    });
  }

  getMoodColor(): string {
    const score = this.moodForm.get('moodScore')?.value || 5;
    const scale = this.moodScales.find(s => s.value === score);
    return scale?.color || '#95a5a6';
  }

  getMoodLabel(): string {
    const score = this.moodForm.get('moodScore')?.value || 5;
    const scale = this.moodScales.find(s => s.value === score);
    return scale?.label || 'Average';
  }

  onSubmit(): void {
    if (!this.moodForm.valid) {
      this.errorMessage = 'Please fill in all required fields correctly';
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'User not logged in';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request = {
      patientId: currentUser.userId,
      ...this.moodForm.value
    };

    if (this.isEditMode && this.editingId) {
      this.monitoringService.updateMoodEntry(this.editingId, request).subscribe({
        next: () => {
          this.successMessage = 'Mood entry updated successfully!';
          this.saving = false;
          setTimeout(() => this.router.navigate(['/monitoring']), 1500);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to update mood entry';
          this.saving = false;
        }
      });
    } else {
      this.monitoringService.createMoodEntry(request).subscribe({
        next: () => {
          this.successMessage = 'Mood entry created successfully!';
          this.saving = false;
          setTimeout(() => this.router.navigate(['/monitoring']), 1500);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to create mood entry';
          this.saving = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/monitoring']);
  }

  getConditionChars(): number {
    return this.moodForm.get('moodDescription')?.value?.length || 0;
  }

  getTriggersChars(): number {
    return this.moodForm.get('triggers')?.value?.length || 0;
  }
}
