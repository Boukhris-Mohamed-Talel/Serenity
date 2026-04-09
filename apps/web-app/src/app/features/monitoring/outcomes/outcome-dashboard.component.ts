import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MonitoringService } from '../../../core/services/monitoring.service';
import { MoodEntryResponse } from '../../../shared/models/mood.model';

interface DoctorOutcomeKpis {
  totalPatients: number;
  averageMood: number;
  averageMoodChange: number;
  crisisEvents: number;
  activeHighRiskPatients: number;
}

@Component({
  selector: 'app-outcome-dashboard',
  templateUrl: './outcome-dashboard.component.html',
  styleUrls: ['./outcome-dashboard.component.scss']
})
export class OutcomeDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';
  kpis: DoctorOutcomeKpis = {
    totalPatients: 0,
    averageMood: 0,
    averageMoodChange: 0,
    crisisEvents: 0,
    activeHighRiskPatients: 0
  };

  constructor(
    private readonly authService: AuthService,
    private readonly monitoringService: MonitoringService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!this.authService.isDoctor() || !currentUser?.userId) {
      this.errorMessage = 'Doctor account not found.';
      this.loading = false;
      return;
    }

    this.monitoringService.getMoodEntriesForDoctor(currentUser.userId).subscribe({
      next: (entries) => {
        this.kpis = this.computeKpis(entries);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || err?.message || 'Failed to load outcomes data';
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/monitoring']);
  }

  private computeKpis(entries: MoodEntryResponse[]): DoctorOutcomeKpis {
    if (!entries.length) {
      return this.kpis;
    }

    const patientMap = new Map<number, MoodEntryResponse[]>();
    let moodSum = 0;
    let crisisEvents = 0;

    for (const entry of entries) {
      moodSum += entry.moodScore;
      if (entry.moodScore <= 3) {
        crisisEvents++;
      }

      if (!patientMap.has(entry.patientId)) {
        patientMap.set(entry.patientId, []);
      }
      patientMap.get(entry.patientId)!.push(entry);
    }

    let totalMoodChange = 0;
    let activeHighRiskPatients = 0;

    patientMap.forEach((patientEntries) => {
      const sorted = [...patientEntries].sort((a, b) =>
        new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );

      const first = sorted[0];
      const latest = sorted[sorted.length - 1];
      totalMoodChange += latest.moodScore - first.moodScore;

      if (latest.moodScore <= 3) {
        activeHighRiskPatients++;
      }
    });

    const totalPatients = patientMap.size;

    return {
      totalPatients,
      averageMood: Number((moodSum / entries.length).toFixed(2)),
      averageMoodChange: Number((totalMoodChange / totalPatients).toFixed(2)),
      crisisEvents,
      activeHighRiskPatients
    };
  }
}

