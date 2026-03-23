import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PatientDefaultPharmacyResponse,
  PharmacyCandidateResponse
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-patient-pharmacy',
  templateUrl: './patient-pharmacy.component.html',
  styleUrls: ['./patient-pharmacy.component.scss']
})
export class PatientPharmacyComponent implements OnInit {
  loading = true;
  nearestLoading = false;
  saving = false;

  errorMessage = '';
  successMessage = '';

  cityFilter = '';
  governorateFilter = '';

  defaultPharmacy: PatientDefaultPharmacyResponse | null = null;
  pharmacies: PharmacyCandidateResponse[] = [];
  nearestCandidates: PharmacyCandidateResponse[] = [];

  ngOnInit(): void {
    this.loadDefaultPharmacy();
    this.loadPharmacies();
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

  loadPharmacies(): void {
    this.loading = true;
    this.errorMessage = '';

    this.pharmacyService.listPatientPharmacies(this.cityFilter, this.governorateFilter).subscribe({
      next: (items) => {
        this.pharmacies = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacies';
        this.loading = false;
      }
    });
  }

  useMyLocation(): void {
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
              this.nearestCandidates = items;
              this.nearestLoading = false;
              if (items.length === 0) {
                this.errorMessage = 'No nearby pharmacies were found in the selected radius.';
              }
            },
            error: (err) => {
              this.errorMessage = err.error?.message || 'Failed to fetch nearest pharmacies';
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
}
