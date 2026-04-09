import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  AdminPharmacyApplicationDetails,
  AdminPharmacyApplicationSummary,
  PharmacyApplicationStatus
} from '../../../shared/models/pharmacy.model';

type FilterValue = 'ALL' | PharmacyApplicationStatus;

@Component({
  selector: 'app-pharmacy-applications',
  templateUrl: './pharmacy-applications.component.html',
  styleUrls: ['./pharmacy-applications.component.scss']
})
export class PharmacyApplicationsComponent implements OnInit {
  readonly filters: FilterValue[] = ['ALL', 'SUBMITTED', 'REJECTED', 'APPROVED'];

  selectedFilter: FilterValue = 'SUBMITTED';
  applications: AdminPharmacyApplicationSummary[] = [];
  selectedDetails: AdminPharmacyApplicationDetails | null = null;

  loadingList = true;
  loadingDetails = false;
  actionLoading = false;

  listErrorMessage = '';
  detailsErrorMessage = '';
  successMessage = '';
  reviewComment = '';

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  setFilter(filter: FilterValue): void {
    if (this.selectedFilter === filter) {
      return;
    }
    this.selectedFilter = filter;
    this.selectedDetails = null;
    this.reviewComment = '';
    this.successMessage = '';
    this.loadApplications();
  }

  loadApplications(): void {
    this.loadingList = true;
    this.listErrorMessage = '';

    const status = this.selectedFilter === 'ALL' ? undefined : this.selectedFilter;
    this.pharmacyService.listAdminPharmacyApplications(status).subscribe({
      next: (items) => {
        this.applications = items;
        this.loadingList = false;
      },
      error: (err) => {
        this.applications = [];
        this.loadingList = false;
        this.listErrorMessage = err.error?.message || 'Failed to load pharmacy applications';
      }
    });
  }

  openDetails(applicationId: number): void {
    this.loadingDetails = true;
    this.detailsErrorMessage = '';
    this.successMessage = '';
    this.reviewComment = '';

    this.pharmacyService.getAdminPharmacyApplicationDetails(applicationId).subscribe({
      next: (details) => {
        this.selectedDetails = details;
        this.loadingDetails = false;
      },
      error: (err) => {
        this.selectedDetails = null;
        this.loadingDetails = false;
        this.detailsErrorMessage = err.error?.message || 'Failed to load application details';
      }
    });
  }

  approveSelected(): void {
    const details = this.selectedDetails;
    if (!details || this.actionLoading) {
      return;
    }
    if (!window.confirm(`Approve application #${details.id} for ${details.pharmacyName}?`)) {
      return;
    }

    this.actionLoading = true;
    this.detailsErrorMessage = '';
    this.successMessage = '';

    this.pharmacyService.approveAdminPharmacyApplication(details.id).subscribe({
      next: (updated) => {
        this.selectedDetails = updated;
        this.actionLoading = false;
        this.successMessage = 'Application approved and PHARMACIST role assigned.';
        this.loadApplications();
      },
      error: (err) => {
        this.actionLoading = false;
        this.detailsErrorMessage = err.error?.message || 'Failed to approve application';
      }
    });
  }

  rejectSelected(): void {
    const details = this.selectedDetails;
    if (!details || this.actionLoading) {
      return;
    }
    const comment = this.reviewComment.trim();
    if (!comment) {
      this.detailsErrorMessage = 'Please provide a rejection comment.';
      return;
    }
    if (!window.confirm(`Reject application #${details.id}?`)) {
      return;
    }

    this.actionLoading = true;
    this.detailsErrorMessage = '';
    this.successMessage = '';

    this.pharmacyService.rejectAdminPharmacyApplication(details.id, comment).subscribe({
      next: (updated) => {
        this.selectedDetails = updated;
        this.reviewComment = '';
        this.actionLoading = false;
        this.successMessage = 'Application rejected successfully.';
        this.loadApplications();
      },
      error: (err) => {
        this.actionLoading = false;
        this.detailsErrorMessage = err.error?.message || 'Failed to reject application';
      }
    });
  }

  openDocument(path?: string): void {
    if (!path) {
      return;
    }
    this.pharmacyService.fetchAdminApplicationDocument(path).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        window.open(objectUrl, '_blank');
      },
      error: () => {
        this.detailsErrorMessage = 'Failed to open document preview.';
      }
    });
  }

  trackByApplicationId(_: number, item: AdminPharmacyApplicationSummary): number {
    return item.id;
  }
}
