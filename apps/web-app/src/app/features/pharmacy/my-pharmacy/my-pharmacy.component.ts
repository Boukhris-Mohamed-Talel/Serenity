import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PharmacyService } from '../../../core/services/pharmacy.service';

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
      latitude: [null],
      longitude: [null],
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
    if (this.form.invalid) return;

    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.pharmacyService.upsertMyPharmacy(this.form.value).subscribe({
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
}
