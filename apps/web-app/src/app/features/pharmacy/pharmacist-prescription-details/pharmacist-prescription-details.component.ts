import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionResponse,
  PrescriptionStatus,
  PrescriptionStatusUpdateRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-pharmacist-prescription-details',
  templateUrl: './pharmacist-prescription-details.component.html',
  styleUrls: ['./pharmacist-prescription-details.component.scss']
})
export class PharmacistPrescriptionDetailsComponent implements OnInit {
  loading = true;
  errorMessage = '';
  prescription: PrescriptionResponse | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/pharmacy/inbox']);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.pharmacyService.getPrescriptionById(id).subscribe({
      next: (item) => {
        this.prescription = item;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load prescription details';
        this.loading = false;
      }
    });
  }

  updateStatus(status: PrescriptionStatus): void {
    if (!this.prescription) {
      return;
    }
    let rejectionReason = '';
    if (status === 'REJECTED') {
      rejectionReason = prompt('Please provide rejection reason') || '';
      if (!rejectionReason.trim()) return;
    }

    const payload: PrescriptionStatusUpdateRequest = { status, rejectionReason };
    this.pharmacyService.updatePrescriptionStatus(this.prescription.id, payload).subscribe({
      next: (updated) => {
        this.prescription = updated;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update prescription status';
      }
    });
  }

  canProcess(status: PrescriptionStatus): boolean {
    return status === 'PENDING' || status === 'ACCEPTED';
  }

  medicineLines(): Array<{ medicationName: string; dosage: string; quantity: number; instructions?: string }> {
    const row = this.prescription;
    if (!row) return [];
    if (row.medicineLines && row.medicineLines.length > 0) {
      return row.medicineLines;
    }

    return [
      {
        medicationName: row.medicationName || '-',
        dosage: row.dosage || '-',
        quantity: row.quantity ?? 0,
        instructions: row.instructions
      }
    ];
  }

  goBack(): void {
    this.router.navigate(['/pharmacy/inbox']);
  }
}
