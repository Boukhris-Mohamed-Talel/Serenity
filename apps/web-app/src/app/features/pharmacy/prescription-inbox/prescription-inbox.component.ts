import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionResponse,
  PrescriptionStatus,
  PrescriptionStatusUpdateRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-prescription-inbox',
  templateUrl: './prescription-inbox.component.html',
  styleUrls: ['./prescription-inbox.component.scss']
})
export class PrescriptionInboxComponent implements OnInit {
  loading = true;
  errorMessage = '';
  prescriptions: PrescriptionResponse[] = [];

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadInbox();
  }

  loadInbox(): void {
    this.loading = true;
    this.pharmacyService.getInbox().subscribe({
      next: (rows) => {
        this.prescriptions = rows;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load prescription inbox';
        this.loading = false;
      }
    });
  }

  updateStatus(row: PrescriptionResponse, status: PrescriptionStatus): void {
    let rejectionReason = '';
    if (status === 'REJECTED') {
      rejectionReason = prompt('Please provide rejection reason') || '';
      if (!rejectionReason.trim()) return;
    }

    const payload: PrescriptionStatusUpdateRequest = { status, rejectionReason };

    this.pharmacyService.updatePrescriptionStatus(row.id, payload).subscribe({
      next: (updated) => {
        const idx = this.prescriptions.findIndex(p => p.id === updated.id);
        if (idx !== -1) this.prescriptions[idx] = updated;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update prescription status';
      }
    });
  }

  canProcess(status: PrescriptionStatus): boolean {
    return status === 'PENDING' || status === 'ACCEPTED';
  }
}
