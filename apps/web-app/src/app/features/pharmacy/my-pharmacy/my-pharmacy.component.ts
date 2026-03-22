import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PharmacyUpsertRequest } from '../../../shared/models/pharmacy.model';
import { PickerLocation } from '../../../shared/components/location-picker/location-picker.component';

@Component({
  selector: 'app-my-pharmacy',
  templateUrl: './my-pharmacy.component.html',
  styleUrls: ['./my-pharmacy.component.scss']
})
export class MyPharmacyComponent implements OnInit {
  form!: FormGroup;
  loading = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      licenseNumber: ['', [Validators.required]],
      phone: [''],
      openingHours: [''],
      addressLine: [''],
      city: [''],
      governorate: [''],
      latitude: [null, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitude: [null, [Validators.required, Validators.min(-180), Validators.max(180)]],
      supportsEmergency: [false]
    });

    this.load();
  }

  load(): void {
    this.loading = true;
    this.pharmacyService.getMyPharmacy().subscribe({
      next: (pharmacy) => {
        this.form.patchValue(pharmacy);
        this.loading = false;
      },
      error: () => {
        // First-time pharmacists may not have a profile yet.
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.get('latitude')?.invalid || this.form.get('longitude')?.invalid) {
        this.errorMessage = 'Please select your pharmacy exact location on the map.';
      }
      return;
    }

    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload: PharmacyUpsertRequest = {
      ...this.form.value,
      latitude: Number(this.form.get('latitude')?.value),
      longitude: Number(this.form.get('longitude')?.value)
    };

    this.pharmacyService.upsertMyPharmacy(payload).subscribe({
      next: () => {
        this.successMessage = 'Pharmacy profile saved successfully.';
        this.saving = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to save pharmacy profile';
        this.saving = false;
      }
    });
  }

  onLocationSelected(location: PickerLocation): void {
    this.form.patchValue({
      latitude: location.latitude,
      longitude: location.longitude
    });
    this.form.get('latitude')?.markAsTouched();
    this.form.get('longitude')?.markAsTouched();
    this.form.get('latitude')?.updateValueAndValidity();
    this.form.get('longitude')?.updateValueAndValidity();
  }
}
