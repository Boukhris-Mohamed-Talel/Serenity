import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PrescriptionResponse } from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-pharmacist-dashboard',
  templateUrl: './pharmacist-dashboard.component.html',
  styleUrls: ['./pharmacist-dashboard.component.scss']
})
export class PharmacistDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';

  total = 0;
  pending = 0;
  ready = 0;
  rejected = 0;

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  private loadStats(): void {
    this.loading = true;
    this.pharmacyService.getInbox().subscribe({
      next: (items) => {
        this.setStats(items);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacist dashboard';
        this.loading = false;
      }
    });
  }

  private setStats(items: PrescriptionResponse[]): void {
    this.total = items.length;
    this.pending = items.filter(i => i.status === 'PENDING').length;
    this.ready = items.filter(i => i.status === 'READY_FOR_PICKUP').length;
    this.rejected = items.filter(i => i.status === 'REJECTED').length;
  }
}
