import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PatientDefaultPharmacyResponse,
  PharmacyCandidateResponse,
  PrescriptionResponse,
  PrescriptionStatus
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-patient-pharmacy',
  templateUrl: './patient-pharmacy.component.html',
  styleUrls: ['./patient-pharmacy.component.scss']
})
export class PatientPharmacyComponent implements OnInit {
  loading = false;
  nearestLoading = false;
  saving = false;
  prescriptionsLoading = true;
  hasSearchedCandidates = false;

  errorMessage = '';
  successMessage = '';
  prescriptionsErrorMessage = '';

  cityFilter = '';
  governorateFilter = '';

  defaultPharmacy: PatientDefaultPharmacyResponse | null = null;
  candidateResults: PharmacyCandidateResponse[] = [];
  prescriptions: PrescriptionResponse[] = [];

  ngOnInit(): void {
    this.loadDefaultPharmacy();
    this.loadPrescriptions();
  }

  constructor(private readonly pharmacyService: PharmacyService) {}

  loadDefaultPharmacy(): void {
    this.pharmacyService.getMyDefaultPharmacy().subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
      },
      error: (err) => {
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load your default pharmacy';
        }
        this.defaultPharmacy = null;
      }
    });
  }

  loadPrescriptions(): void {
    this.prescriptionsLoading = true;
    this.prescriptionsErrorMessage = '';

    this.pharmacyService.getMyPrescriptions().subscribe({
      next: (items) => {
        this.prescriptions = items;
        this.prescriptionsLoading = false;
      },
      error: (err) => {
        this.prescriptionsErrorMessage = err.error?.message || 'Failed to load your prescriptions';
        this.prescriptionsLoading = false;
      }
    });
  }

  loadPharmacies(): void {
    this.loading = true;
    this.hasSearchedCandidates = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.listPatientPharmacies(this.cityFilter, this.governorateFilter).subscribe({
      next: (items) => {
        this.candidateResults = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacies';
        this.candidateResults = [];
        this.loading = false;
      }
    });
  }

  useMyLocation(): void {
    this.hasSearchedCandidates = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (!navigator.geolocation) {
      this.errorMessage = 'Geolocation is not supported by this browser. Choose from the pharmacy list instead.';
      return;
    }

    this.nearestLoading = true;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.pharmacyService
          .suggestNearestPharmacies(position.coords.latitude, position.coords.longitude)
          .subscribe({
            next: (items) => {
              this.candidateResults = items;
              this.nearestLoading = false;
              if (items.length === 0) {
                this.errorMessage = 'No nearby pharmacies were found in the selected radius.';
              }
            },
            error: (err) => {
              this.errorMessage = err.error?.message || 'Failed to fetch nearest pharmacies';
              this.candidateResults = [];
              this.nearestLoading = false;
            }
          });
      },
      () => {
        this.nearestLoading = false;
        this.errorMessage = 'Location permission was denied. Please choose from the list below.';
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  setDefault(pharmacyId: number): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.setMyDefaultPharmacy({ pharmacyId }).subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
        this.saving = false;
        this.successMessage = 'Default pharmacy updated successfully.';
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to set default pharmacy';
        this.saving = false;
      }
    });
  }

  isDefault(pharmacyId: number): boolean {
    return this.defaultPharmacy?.pharmacyId === pharmacyId;
  }

  isCandidatesLoading(): boolean {
    return this.loading || this.nearestLoading;
  }

  statusClass(status: PrescriptionStatus): string {
    return `status ${status.toLowerCase()}`;
  }

  isReadyForPickup(status: PrescriptionStatus): boolean {
    return status === 'READY_FOR_PICKUP';
  }
}
