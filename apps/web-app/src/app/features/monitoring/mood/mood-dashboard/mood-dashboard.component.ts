import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { EmotionalTriggerResponse, MoodEntryResponse } from '../../../../shared/models/mood.model';

interface PatientDot {
  patientId: number;
  patientName: string;
  avatarUrl?: string;
  entries: MoodEntryResponse[];
  latestMood: number;
  latestDate: Date;
  averageMood: number;
  moodTrend: number;
  triggerLoad: number;
  crisisRate: number;
  riskScore: number;
  x: number;
  y: number;
}

@Component({
  selector: 'app-mood-dashboard',
  templateUrl: './mood-dashboard.component.html',
  styleUrls: ['./mood-dashboard.component.scss']
})
export class MoodDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';

  chartWidth = 1100;
  chartHeight = 380;
  padding = 52;

  doctorId: number | null = null;
  patientDots: PatientDot[] = [];
  selectedPatient: PatientDot | null = null;
  hoveredPatient: PatientDot | null = null;

  private triggerByMoodEntryId: Record<number, EmotionalTriggerResponse[]> = {};
  private readonly moodMin = 1;
  private readonly moodMax = 10;

  constructor(
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!this.authService.isDoctor() || !currentUser?.userId) {
      this.router.navigate(['/monitoring']);
      return;
    }

    this.doctorId = currentUser.userId;
    this.loadDashboard(currentUser.userId);
  }

  get xAxisY(): number {
    return this.chartHeight - this.padding;
  }

  get yAxisX(): number {
    return this.padding;
  }

  get linePath(): string {
    if (this.patientDots.length === 0) {
      return '';
    }

    const pts = this.patientDots;
    let d = `M ${pts[0].x} ${pts[0].y}`;
    for (let i = 1; i < pts.length; i++) {
      d += ` L ${pts[i].x} ${pts[i].y}`;
    }
    return d;
  }

  get areaPath(): string {
    if (!this.linePath || this.patientDots.length === 0) {
      return '';
    }
    const first = this.patientDots[0];
    const last = this.patientDots[this.patientDots.length - 1];
    return `${this.linePath} L ${last.x} ${this.xAxisY} L ${first.x} ${this.xAxisY} Z`;
  }

  get yTicks(): number[] {
    return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  }

  get portfolioAverageMood(): number {
    if (!this.patientDots.length) {
      return 0;
    }
    return this.patientDots.reduce((acc, p) => acc + p.averageMood, 0) / this.patientDots.length;
  }

  get highRiskCount(): number {
    return this.patientDots.filter((p) => p.riskScore >= 70).length;
  }

  get crisisPatientCount(): number {
    return this.patientDots.filter((p) => p.crisisRate > 0).length;
  }

  get selectedPatientName(): string {
    return this.selectedPatient?.patientName || 'No patient selected';
  }

  get rankedPatients(): PatientDot[] {
    return [...this.patientDots].sort((a, b) => b.riskScore - a.riskScore);
  }

  goBack(): void {
    this.router.navigate(['/monitoring']);
  }

  selectPatient(patient: PatientDot): void {
    this.selectedPatient = patient;
  }

  getTrendLabel(value: number): string {
    if (value > 0.15) return `+${value.toFixed(2)} improving`;
    if (value < -0.15) return `${value.toFixed(2)} declining`;
    return 'stable';
  }

  moodLevel(rawMoodScore: number): number {
    return this.normalizeMood(rawMoodScore);
  }

  getRiskBand(risk: number): string {
    if (risk >= 70) return 'High risk';
    if (risk >= 40) return 'Watch';
    return 'Stable';
  }

  getRiskClass(risk: number): string {
    if (risk >= 70) return 'risk-high';
    if (risk >= 40) return 'risk-medium';
    return 'risk-low';
  }

  get hoveredPatientSnapshot(): {
    patientName: string;
    latestMood: number;
    averageMood: number;
    riskScore: number;
    moodTrend: number;
  } {
    return {
      patientName: this.hoveredPatient?.patientName || 'Unknown patient',
      latestMood: this.hoveredPatient?.latestMood ?? 0,
      averageMood: this.hoveredPatient?.averageMood ?? 0,
      riskScore: this.hoveredPatient?.riskScore ?? 0,
      moodTrend: this.hoveredPatient?.moodTrend ?? 0
    };
  }

  getEntryTriggers(entryId: number): EmotionalTriggerResponse[] {
    return this.triggerByMoodEntryId[entryId] || [];
  }

  private loadDashboard(doctorId: number): void {
    this.loading = true;
    this.errorMessage = '';

    this.monitoringService.getMoodEntriesForDoctor(doctorId).pipe(
      switchMap((entries) => {
        if (!entries.length) {
          return of({ entries, triggersByEntry: {} as Record<number, EmotionalTriggerResponse[]> });
        }

        const requests = entries.map((entry) =>
          this.monitoringService.getTriggersByMoodEntryId(entry.id).pipe(
            map((triggers) => ({ moodEntryId: entry.id, triggers })),
            catchError(() => of({ moodEntryId: entry.id, triggers: [] as EmotionalTriggerResponse[] }))
          )
        );

        return forkJoin(requests).pipe(
          map((triggerResults) => {
            const triggerMap: Record<number, EmotionalTriggerResponse[]> = {};
            triggerResults.forEach((item) => {
              triggerMap[item.moodEntryId] = item.triggers;
            });
            return { entries, triggersByEntry: triggerMap };
          })
        );
      })
    ).subscribe({
      next: ({ entries, triggersByEntry }) => {
        this.triggerByMoodEntryId = triggersByEntry;
        this.patientDots = this.buildPatientDots(entries, triggersByEntry);

        const requestedPatientIdRaw = this.route.snapshot.queryParamMap.get('patientId');
        const requestedPatientId = requestedPatientIdRaw ? Number(requestedPatientIdRaw) : NaN;
        this.selectedPatient = this.patientDots.find((p) => p.patientId === requestedPatientId) || this.rankedPatients[0] || null;

        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to load doctor dashboard.';
      }
    });
  }

  private buildPatientDots(
    entries: MoodEntryResponse[],
    triggerMap: Record<number, EmotionalTriggerResponse[]>
  ): PatientDot[] {
    if (!entries.length) {
      return [];
    }

    const grouped = new Map<number, MoodEntryResponse[]>();

    entries.forEach((entry) => {
      if (!grouped.has(entry.patientId)) {
        grouped.set(entry.patientId, []);
      }
      grouped.get(entry.patientId)!.push(entry);
    });

    const minDateMs = Math.min(...entries.map((e) => +new Date(e.createdAt)));
    const maxDateMs = Math.max(...entries.map((e) => +new Date(e.createdAt)));
    const rangeMs = Math.max(1, maxDateMs - minDateMs);

    const dots = Array.from(grouped.entries()).map(([patientId, patientEntries]) => {
      const sorted = [...patientEntries].sort((a, b) => +new Date(a.createdAt) - +new Date(b.createdAt));
      // Requirement: each dot Y is the average mood score for that patient over all their entries.
      const moods = sorted.map((e) => this.normalizeMood(e.moodScore));
      const averageMood = this.average(moods);
      const moodTrend = moods.length > 1 ? moods[moods.length - 1] - moods[0] : 0;
      const crisisRate = moods.length > 0 ? moods.filter((m) => m <= 3).length / moods.length : 0;
      const triggerValues = sorted.flatMap((e) => (triggerMap[e.id] || []).map((t) => t.intensity));
      const triggerLoad = triggerValues.length ? this.average(triggerValues) : 0;

      const latest = sorted[sorted.length - 1];
      const latestMood = this.normalizeMood(latest.moodScore);
      const latestDate = new Date(latest.createdAt);

      const lowMoodFactor = (this.moodMax - latestMood) / (this.moodMax - this.moodMin);
      const triggerFactor = triggerLoad / 10;
      const negativeTrendFactor = moodTrend < 0 ? Math.min(Math.abs(moodTrend) / 4, 1) : 0;
      const riskScore = this.clamp(
        100 * (0.35 * lowMoodFactor + 0.30 * crisisRate + 0.20 * triggerFactor + 0.15 * negativeTrendFactor),
        0,
        100
      );

      const x = this.padding + ((+latestDate - minDateMs) / rangeMs) * (this.chartWidth - this.padding * 2);
      const normalizedY = (averageMood - this.moodMin) / (this.moodMax - this.moodMin);
      const y = this.chartHeight - this.padding - normalizedY * (this.chartHeight - this.padding * 2);

      return {
        patientId,
        patientName: patientEntries[patientEntries.length - 1].patientName || `Patient #${patientId}`,
        avatarUrl: patientEntries[patientEntries.length - 1].patientAvatarUrl || undefined,
        crisisRate,
        latestMood,
        latestDate,
        averageMood,
        moodTrend,
        triggerLoad,
        riskScore,
        entries: sorted,
        x,
        y
      } as PatientDot;
    });

    return dots.sort((a, b) => +a.latestDate - +b.latestDate);
  }

  private average(values: number[]): number {
    if (!values.length) {
      return 0;
    }
    return values.reduce((acc, v) => acc + v, 0) / values.length;
  }

  private normalizeMood(rawMoodScore: number): number {
    return this.clamp(rawMoodScore, this.moodMin, this.moodMax);
  }

  private getTriggerPenalty(triggers: EmotionalTriggerResponse[]): number {
    if (!triggers.length) {
      return 0;
    }
    const avgIntensity = this.average(triggers.map((t) => t.intensity));
    // Max penalty is 0.4 mood points when trigger load is severe.
    return this.clamp(avgIntensity / 25, 0, 0.4);
  }


  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }
}

