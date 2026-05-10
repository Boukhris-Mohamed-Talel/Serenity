import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

type LoaderRole = 'doctor' | 'pharmacist' | 'patient' | 'default';

interface RoleConfig {
  key: LoaderRole;
  themeClass: string;
  badgeIcon: string;
  greeting: string;
  subtitle: string;
  steps: string[];
}

@Component({
  selector: 'app-role-loading',
  templateUrl: './role-loading.component.html',
  styleUrls: ['./role-loading.component.scss']
})
export class RoleLoadingComponent implements OnInit, OnDestroy {
  config!: RoleConfig;
  currentStep = 0;
  progress = 0;
  done = false;

  private stepTimer?: ReturnType<typeof setInterval>;
  private progressTimer?: ReturnType<typeof setInterval>;
  private redirectTimer?: ReturnType<typeof setTimeout>;

  /** Total visible duration of the loader before redirect (ms). */
  private readonly DURATION_MS = 2800;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const user = this.authService.getCurrentUser();
    const role = this.normalizeRole(user?.role);
    this.config = this.buildConfig(role);

    this.startStepTicker();
    this.startProgressTicker();

    this.redirectTimer = setTimeout(() => {
      this.done = true;
      this.currentStep = this.config.steps.length - 1;
      this.progress = 100;
      setTimeout(() => this.redirect(), 350);
    }, this.DURATION_MS);
  }

  ngOnDestroy(): void {
    if (this.stepTimer) clearInterval(this.stepTimer);
    if (this.progressTimer) clearInterval(this.progressTimer);
    if (this.redirectTimer) clearTimeout(this.redirectTimer);
  }

  private startStepTicker(): void {
    const interval = Math.floor(this.DURATION_MS / 4);
    this.stepTimer = setInterval(() => {
      if (this.currentStep < this.config.steps.length - 1) {
        this.currentStep++;
      }
    }, interval);
  }

  private startProgressTicker(): void {
    const tickMs = 40;
    const increment = (100 / this.DURATION_MS) * tickMs;
    this.progressTimer = setInterval(() => {
      if (this.progress < 96) {
        this.progress = Math.min(96, this.progress + increment);
      }
    }, tickMs);
  }

  private redirect(): void {
    const destination = this.authService.isAdmin() ? '/admin' : '/';
    this.router.navigate([destination]);
  }

  private normalizeRole(role: string | null | undefined): LoaderRole {
    const value = (role ?? '').trim().toUpperCase().replace(/^ROLE_/, '');
    if (value === 'DOCTOR') return 'doctor';
    if (value === 'PHARMACIST') return 'pharmacist';
    if (value === 'PATIENT') return 'patient';
    return 'default';
  }

  private buildConfig(role: LoaderRole): RoleConfig {
    switch (role) {
      case 'doctor':
        return {
          key: 'doctor',
          themeClass: 'theme-doctor',
          badgeIcon: 'health_and_safety',
          greeting: 'Preparing your clinic',
          subtitle: 'Reviewing today\u2019s schedule and patient charts',
          steps: [
            'Securing your session\u2026',
            'Loading patient charts\u2026',
            'Syncing today\u2019s appointments\u2026',
            'Ready when you are'
          ]
        };
      case 'pharmacist':
        return {
          key: 'pharmacist',
          themeClass: 'theme-pharmacist',
          badgeIcon: 'local_pharmacy',
          greeting: 'Opening your pharmacy',
          subtitle: 'Stocking shelves and queueing prescriptions',
          steps: [
            'Securing your session\u2026',
            'Updating inventory\u2026',
            'Loading prescription queue\u2026',
            'Pharmacy is open'
          ]
        };
      case 'patient':
        return {
          key: 'patient',
          themeClass: 'theme-patient',
          badgeIcon: 'spa',
          greeting: 'Welcome back',
          subtitle: 'Gathering your wellness journey',
          steps: [
            'Securing your session\u2026',
            'Restoring your dashboard\u2026',
            'Loading appointments and mood diary\u2026',
            'All set'
          ]
        };
      default:
        return {
          key: 'default',
          themeClass: 'theme-default',
          badgeIcon: 'spa',
          greeting: 'Welcome to Serenity',
          subtitle: 'Setting things up for you',
          steps: [
            'Securing your session\u2026',
            'Loading workspace\u2026',
            'Almost there\u2026',
            'Ready'
          ]
        };
    }
  }
}
