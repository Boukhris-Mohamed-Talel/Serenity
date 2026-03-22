import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { StockItemResponse } from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-stock-management',
  templateUrl: './stock-management.component.html',
  styleUrls: ['./stock-management.component.scss']
})
export class StockManagementComponent implements OnInit {
  loading = true;
  errorMessage = '';
  successMessage = '';

  query = '';
  includeArchived = false;
  incrementValue: Record<number, number> = {};

  items: StockItemResponse[] = [];

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadStock();
  }

  loadStock(): void {
    this.loading = true;
    this.errorMessage = '';

    this.pharmacyService.listStock(this.query, this.includeArchived).subscribe({
      next: (items) => {
        this.items = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load stock items';
        this.loading = false;
      }
    });
  }

  increment(item: StockItemResponse): void {
    const incrementBy = this.incrementValue[item.id] || 1;
    if (incrementBy < 1) {
      return;
    }

    this.pharmacyService.incrementStockItem(item.id, { incrementBy }).subscribe({
      next: (updated) => {
        this.successMessage = `${updated.medicineName} quantity updated`;
        this.replaceItem(updated);
        this.incrementValue[item.id] = 1;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to increment stock quantity';
      }
    });
  }

  markOutOfStock(item: StockItemResponse): void {
    this.pharmacyService.markOutOfStock(item.id).subscribe({
      next: (updated) => {
        this.successMessage = `${updated.medicineName} marked as out of stock`;
        this.replaceItem(updated);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to mark as out of stock';
      }
    });
  }

  archive(item: StockItemResponse): void {
    this.pharmacyService.archiveStockItem(item.id).subscribe({
      next: () => {
        this.successMessage = `${item.medicineName} archived`;
        this.items = this.items.filter(x => x.id !== item.id);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to archive medicine';
      }
    });
  }

  trackById(_: number, item: StockItemResponse): number {
    return item.id;
  }

  private replaceItem(updated: StockItemResponse): void {
    this.items = this.items.map(item => (item.id === updated.id ? updated : item));
  }
}
