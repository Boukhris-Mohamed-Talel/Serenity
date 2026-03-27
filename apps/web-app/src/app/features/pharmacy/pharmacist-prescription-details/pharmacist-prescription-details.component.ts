import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionLineResponse,
  PrescriptionResponse,
  StockItemResponse,
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
  private stockByMedicine = new Map<string, number>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    this.loadStockSnapshot();
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

  medicineLines(): PrescriptionLineResponse[] {
    const row = this.prescription;
    if (!row) return [];
    if (row.medicineLines && row.medicineLines.length > 0) {
      return row.medicineLines;
    }

    return [
      {
        id: row.id,
        medicationName: row.medicationName || '-',
        dosage: row.dosage || '-',
        quantity: row.quantity ?? 0,
        instructions: row.instructions
      }
    ];
  }

  stockMessageForLine(line: { medicationName: string; quantity: number }): string {
    const available = this.stockQuantityFor(line.medicationName);
    if (available == null) {
      return `In stock: not found | Prescribed: ${line.quantity}`;
    }
    return `In stock: ${available} | Prescribed: ${line.quantity}`;
  }

  goBack(): void {
    this.router.navigate(['/pharmacy/inbox']);
  }

  private loadStockSnapshot(): void {
    this.pharmacyService.listStock(undefined, false).subscribe({
      next: (items) => {
        this.stockByMedicine = this.buildStockLookup(items);
      },
      error: () => {
        this.stockByMedicine.clear();
      }
    });
  }

  private buildStockLookup(items: StockItemResponse[]): Map<string, number> {
    const lookup = new Map<string, number>();
    for (const item of items) {
      if (item.archived) {
        continue;
      }

      const key = this.normalizeMedicineName(item.medicineName);
      if (!key) {
        continue;
      }

      const quantity = Number.isFinite(item.quantity) ? Math.max(0, item.quantity) : 0;
      lookup.set(key, (lookup.get(key) ?? 0) + quantity);
    }
    return lookup;
  }

  private stockQuantityFor(medicationName?: string): number | null {
    const key = this.normalizeMedicineName(medicationName);
    if (!key || !this.stockByMedicine.has(key)) {
      return null;
    }
    return this.stockByMedicine.get(key) ?? null;
  }

  private normalizeMedicineName(value?: string): string {
    return (value ?? '').trim().toLowerCase();
  }
}
