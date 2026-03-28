import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PickerMarker } from '../../../shared/components/location-picker/location-picker.component';
import {
  PatientDefaultPharmacyResponse,
  PharmacyCandidateResponse,
  PrescriptionLineResponse,
  PrescriptionResponse
} from '../../../shared/models/pharmacy.model';

interface PrescriptionCardView {
  raw: PrescriptionResponse;
  lines: PrescriptionLineResponse[];
  primaryLine: PrescriptionLineResponse;
  extraLinesCount: number;
}

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
  prescriptionCards: PrescriptionCardView[] = [];
  mapMarkers: PickerMarker[] = [];
  mapMessage = 'Map will show your default pharmacy location.';

  ngOnInit(): void {
    this.loadDefaultPharmacy();
    this.loadPrescriptions();
  }

  constructor(
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {}

  loadDefaultPharmacy(): void {
    this.pharmacyService.getMyDefaultPharmacy().subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
        this.refreshMapMarkers();
      },
      error: (err) => {
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load your default pharmacy';
        }
        this.defaultPharmacy = null;
        this.refreshMapMarkers();
      }
    });
  }

  loadPrescriptions(): void {
    this.prescriptionsLoading = true;
    this.prescriptionsErrorMessage = '';

    this.pharmacyService.getMyPrescriptions().subscribe({
      next: (items) => {
        this.prescriptionCards = items.map((item) => this.toPrescriptionCard(item));
        this.prescriptionsLoading = false;
      },
      error: (err) => {
        this.prescriptionCards = [];
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
        this.refreshMapMarkers();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacies';
        this.candidateResults = [];
        this.refreshMapMarkers();
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
              this.refreshMapMarkers();
              this.nearestLoading = false;
              if (items.length === 0) {
                this.errorMessage = 'No nearby pharmacies were found in the selected radius.';
              }
            },
            error: (err) => {
              this.errorMessage = err.error?.message || 'Failed to fetch nearest pharmacies';
              this.candidateResults = [];
              this.refreshMapMarkers();
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
    const selectedPharmacy = this.candidateResults.find((candidate) => candidate.id === pharmacyId);
    const pharmacyName = selectedPharmacy?.name || 'this pharmacy';
    if (!window.confirm(`Set "${pharmacyName}" as your default pharmacy?`)) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.setMyDefaultPharmacy({ pharmacyId }).subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
        // Keep map lean and focused after update: show only the selected default pharmacy.
        this.candidateResults = [];
        this.hasSearchedCandidates = false;
        this.refreshMapMarkers();
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

  openPrescriptionDetails(card: PrescriptionCardView): void {
    this.router.navigate(['/pharmacy/patient/prescriptions', card.raw.id]);
  }

  trackByCandidateId(_: number, item: PharmacyCandidateResponse): number {
    return item.id;
  }

  trackByPrescriptionId(_: number, item: PrescriptionCardView): number {
    return item.raw.id;
  }

  private toPrescriptionCard(item: PrescriptionResponse): PrescriptionCardView {
    const lines = item.medicineLines && item.medicineLines.length > 0
      ? item.medicineLines
      : [{
          id: item.id,
          medicationName: item.medicationName || '-',
          dosage: item.dosage || '-',
          quantity: item.quantity ?? 0,
          instructions: item.instructions
        }];

    return {
      raw: item,
      lines,
      primaryLine: lines[0],
      extraLinesCount: Math.max(lines.length - 1, 0)
    };
  }

  private refreshMapMarkers(): void {
    if (this.hasSearchedCandidates) {
      const markers = this.candidateResults
        .filter((candidate) => this.hasCoordinates(candidate.latitude, candidate.longitude))
        .slice(0, 30)
        .map((candidate) => ({
          latitude: candidate.latitude as number,
          longitude: candidate.longitude as number,
          label: candidate.name,
          primary: this.isDefault(candidate.id)
        }));

      this.mapMarkers = markers;

      if (markers.length > 0) {
        this.mapMessage = `${markers.length} pharmacy location${markers.length > 1 ? 's' : ''} shown on the map.`;
        return;
      }

      this.mapMessage = this.candidateResults.length > 0
        ? 'Search results have no map coordinates to display.'
        : 'No pharmacy locations to display for this search yet.';
      return;
    }

    if (this.defaultPharmacy && this.hasCoordinates(this.defaultPharmacy.latitude, this.defaultPharmacy.longitude)) {
      this.mapMarkers = [{
        latitude: this.defaultPharmacy.latitude as number,
        longitude: this.defaultPharmacy.longitude as number,
        label: this.defaultPharmacy.pharmacyName,
        primary: true
      }];
      this.mapMessage = 'Showing your current default pharmacy location.';
      return;
    }

    this.mapMarkers = [];
    this.mapMessage = 'No pharmacy coordinates are available yet.';
  }

  private hasCoordinates(latitude?: number, longitude?: number): boolean {
    return typeof latitude === 'number' && Number.isFinite(latitude)
      && typeof longitude === 'number' && Number.isFinite(longitude);
  }
}
