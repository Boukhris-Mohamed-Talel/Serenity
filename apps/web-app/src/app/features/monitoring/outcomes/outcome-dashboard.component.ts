import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MonitoringService } from '../../../core/services/monitoring.service';
import { DoctorMonitoringDashboard, PatientMoodPoint } from '../../../shared/models/mood.model';

@Component({
  selector: 'app-outcome-dashboard',
  templateUrl: './outcome-dashboard.component.html',
  styleUrls: ['./outcome-dashboard.component.scss']
})
export class OutcomeDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';
  dashboard: DoctorMonitoringDashboard | null = null;
  hoveredTrendIndex: number | null = null;

  readonly chartWidth = 960;
  readonly chartHeight = 360;
  readonly chartPadding = 54;
  readonly yTicks = [10, 8, 6, 4, 2, 1];

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

    this.monitoringService.getDoctorDashboardAnalytics(currentUser.userId).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || err?.message || 'Failed to load analytics data';
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/monitoring']);
  }

  get patientPoints(): PatientMoodPoint[] {
    return this.dashboard?.patientPoints ?? [];
  }

  get trendPoints() {
    return this.dashboard?.moodTrend ?? [];
  }

  get hasTrendData(): boolean {
    return this.trendPoints.length > 0;
  }

  get chartPoints(): Array<{ x: number; y: number; mood: number; label: string; crisisCount: number }> {
    const points = this.trendPoints;
    if (!points.length) {
      return [];
    }

    const usableWidth = this.chartWidth - this.chartPadding * 2;
    const usableHeight = this.chartHeight - this.chartPadding * 2;
    const maxIndex = Math.max(points.length - 1, 1);

    return points.map((point, index) => {
      const x = this.chartPadding + (index / maxIndex) * usableWidth;
      const normalizedMood = Math.min(Math.max(point.averageMood, 1), 10);
      const y = this.chartPadding + ((10 - normalizedMood) / 9) * usableHeight;
      return {
        x,
        y,
        mood: normalizedMood,
        label: point.date,
        crisisCount: point.crisisCount
      };
    });
  }

  get chartPath(): string {
    const points = this.chartPoints;
    if (points.length === 0) {
      return '';
    }
    return this.buildSmoothPath(points);
  }

  get areaPath(): string {
    const points = this.chartPoints;
    if (!points.length) {
      return '';
    }

    const baseY = this.chartHeight - this.chartPadding;
    return `${this.chartPath} L ${points[points.length - 1].x} ${baseY} L ${points[0].x} ${baseY} Z`;
  }

  get hoveredPoint() {
    if (this.hoveredTrendIndex === null) {
      return null;
    }
    return this.chartPoints[this.hoveredTrendIndex] ?? null;
  }

  get trendSummary(): string {
    const points = this.trendPoints;
    if (!points.length) {
      return 'No trend yet';
    }

    const first = points[0].averageMood;
    const last = points[points.length - 1].averageMood;
    const delta = Number((last - first).toFixed(2));
    return `${points.length} points | ${delta >= 0 ? '+' : ''}${delta} overall mood change`;
  }

  moodBadgeClass(score: number): string {
    if (score <= 3) {
      return 'critical';
    }
    if (score <= 6) {
      return 'medium';
    }
    return 'good';
  }

  private buildSmoothPath(points: Array<{ x: number; y: number }>): string {
    if (points.length === 1) {
      return `M ${points[0].x} ${points[0].y}`;
    }

    if (points.length === 2) {
      const [p1, p2] = points;
      const cx = (p1.x + p2.x) / 2;
      const cy = ((p1.y + p2.y) / 2) - 26;
      return `M ${p1.x} ${p1.y} Q ${cx} ${cy} ${p2.x} ${p2.y}`;
    }

    let path = `M ${points[0].x} ${points[0].y}`;

    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[i - 1] ?? points[i];
      const p1 = points[i];
      const p2 = points[i + 1];
      const p3 = points[i + 2] ?? p2;

      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;

      path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
    }

    return path;
  }
}

