import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { UserService } from '../../../core/services/user.service';
import { PickerLocation } from '../../../shared/components/location-picker/location-picker.component';
import {
  PharmacyApplicationResponse,
  PharmacyApplicationSubmitRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-pharmacy-application',
  templateUrl: './pharmacy-application.component.html',
  styleUrls: ['./pharmacy-application.component.scss']
})
export class PharmacyApplicationComponent implements OnInit {
  loading = true;
  submitting = false;

  errorMessage = '';
  successMessage = '';

  application: PharmacyApplicationResponse | null = null;

  cinDocumentFile: File | null = null;
  cnoptProofDocumentFile: File | null = null;
  legalProofDocumentFile: File | null = null;

  readonly applicationForm = this.formBuilder.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    cinNumber: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern(/^\d{8}$/)]],
    cnopNumber: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(40)]],
    pharmacyName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    authorizationReferenceNumber: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(80)]],
    phone: ['', [Validators.pattern(/^\d{8}$/)]],
    openingHours: ['', [Validators.maxLength(120)]],
    addressLine: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(255)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    governorate: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    latitude: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly pharmacyService: PharmacyService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.prefillIdentityFromCurrentUser();
    this.loadMyApplication();
  }

  get isReadOnly(): boolean {
    return this.application?.status === 'SUBMITTED' || this.application?.status === 'APPROVED';
  }

  get canSubmit(): boolean {
    return !this.loading && !this.submitting && !this.isReadOnly;
  }

  onLocationSelected(location: PickerLocation): void {
    this.applicationForm.patchValue({
      latitude: location.latitude,
      longitude: location.longitude
    });
  }

  onFileSelected(event: Event, type: 'cin' | 'cnopt' | 'legal'): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (type === 'cin') {
      this.cinDocumentFile = file;
      return;
    }
    if (type === 'cnopt') {
      this.cnoptProofDocumentFile = file;
      return;
    }
    this.legalProofDocumentFile = file;
  }

  submit(): void {
    if (!this.canSubmit) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.applicationForm.markAllAsTouched();

    if (this.applicationForm.invalid) {
      this.errorMessage = 'Please complete all required fields before submitting.';
      return;
    }

    if (!this.hasAllRequiredDocuments()) {
      this.errorMessage = 'Please provide the 3 required documents before submitting.';
      return;
    }

    const payload = this.applicationForm.getRawValue() as PharmacyApplicationSubmitRequest;
    this.submitting = true;

    this.pharmacyService.submitMyPharmacyApplication(
      payload,
      this.cinDocumentFile,
      this.cnoptProofDocumentFile,
      this.legalProofDocumentFile
    ).subscribe({
      next: (response) => {
        this.application = response;
        this.submitting = false;
        this.successMessage = 'Application submitted successfully. You can continue using the app while waiting for review.';
        this.applicationForm.disable();
        this.resetLocalFiles();
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || 'Failed to submit application';
      }
    });
  }

  continueAsPatient(): void {
    this.router.navigate(['/pharmacy/patient']);
  }

  goToPharmacistWorkspace(): void {
    this.router.navigate(['/pharmacy']);
  }

  private loadMyApplication(): void {
    this.loading = true;
    this.pharmacyService.getMyPharmacyApplication().subscribe({
      next: (application) => {
        this.application = application;
        this.patchFormFromApplication(application);
        this.loading = false;

        if (this.isReadOnly) {
          this.applicationForm.disable();
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load your pharmacist application.';
        }
      }
    });
  }

  private patchFormFromApplication(application: PharmacyApplicationResponse): void {
    this.applicationForm.patchValue({
      firstName: application.firstName || '',
      lastName: application.lastName || '',
      email: application.email || '',
      cinNumber: application.cinNumber || '',
      cnopNumber: application.cnopNumber || '',
      pharmacyName: application.pharmacyName || '',
      authorizationReferenceNumber: application.authorizationReferenceNumber || '',
      phone: application.phone || '',
      openingHours: application.openingHours || '',
      addressLine: application.addressLine || '',
      city: application.city || '',
      governorate: application.governorate || '',
      latitude: application.latitude ?? null,
      longitude: application.longitude ?? null
    });
  }

  private prefillIdentityFromCurrentUser(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.applicationForm.patchValue({
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || ''
        });
      },
      error: () => {
        // Keep the form usable even if user profile loading fails.
      }
    });
  }

  private hasAllRequiredDocuments(): boolean {
    const cinReady = !!this.cinDocumentFile || !!this.application?.cinDocumentUploaded;
    const cnoptReady = !!this.cnoptProofDocumentFile || !!this.application?.cnoptProofUploaded;
    const legalReady = !!this.legalProofDocumentFile || !!this.application?.legalDocumentUploaded;
    return cinReady && cnoptReady && legalReady;
  }

  private resetLocalFiles(): void {
    this.cinDocumentFile = null;
    this.cnoptProofDocumentFile = null;
    this.legalProofDocumentFile = null;
  }
}
