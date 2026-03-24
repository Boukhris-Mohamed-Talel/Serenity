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
  selectedPrescription: PrescriptionResponse | null = null;

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadInbox();
  }

  loadInbox(): void {
    this.loading = true;
    this.pharmacyService.getInbox().subscribe({
      next: (rows) => {
        this.prescriptions = rows;
        if (this.selectedPrescription) {
          const refreshed = rows.find(row => row.id === this.selectedPrescription?.id) || null;
          this.selectedPrescription = refreshed;
        }
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
        if (this.selectedPrescription?.id === updated.id) {
          this.selectedPrescription = updated;
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update prescription status';
      }
    });
  }

  canProcess(status: PrescriptionStatus): boolean {
    return status === 'PENDING' || status === 'ACCEPTED';
  }

  openPrescription(row: PrescriptionResponse): void {
    this.selectedPrescription = row;
  }

  closeDetails(): void {
    this.selectedPrescription = null;
  }

  medicineLines(row: PrescriptionResponse): Array<{ medicationName: string; dosage: string; quantity: number; instructions?: string }> {
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

  summaryMedication(row: PrescriptionResponse): string {
    const lines = this.medicineLines(row);
    return lines[0]?.medicationName || '-';
  }

  summaryDosage(row: PrescriptionResponse): string {
    const lines = this.medicineLines(row);
    return lines[0]?.dosage || '-';
  }

  summaryQuantity(row: PrescriptionResponse): number | string {
    const lines = this.medicineLines(row);
    return lines[0]?.quantity ?? '-';
  }

  extraLinesCount(row: PrescriptionResponse): number {
    const lines = this.medicineLines(row);
    return lines.length > 1 ? lines.length - 1 : 0;
  }
}
