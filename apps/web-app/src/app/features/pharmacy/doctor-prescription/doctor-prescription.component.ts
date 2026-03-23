import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { UserService } from '../../../core/services/user.service';
import {
  DoctorMedicineSuggestionItem,
  DoctorMedicineSuggestionResponse,
  PrescriptionCreateRequest,
  PrescriptionResponse
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-doctor-prescription',
  templateUrl: './doctor-prescription.component.html',
  styleUrls: ['./doctor-prescription.component.scss']
})
export class DoctorPrescriptionComponent implements OnInit {
  form: FormGroup;

  saving = false;
  errorMessage = '';
  successMessage = '';

  defaultPharmacyResolved = true;
  defaultPharmacyName = '';
  defaultPharmacyWarning = '';

  lineSuggestions: Record<number, DoctorMedicineSuggestionItem[]> = {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly pharmacyService: PharmacyService,
    private readonly userService: UserService
  ) {
    this.form = this.fb.group({
      patientId: [null, [Validators.required, Validators.min(1)]],
      patientName: ['', [Validators.required, Validators.minLength(2)]],
      doctorName: ['', [Validators.required, Validators.minLength(2)]],
      medicineLines: this.fb.array([this.createLineGroup()])
    });
  }

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        const fullName = `${user.firstName} ${user.lastName}`.trim();
        if (fullName) {
          this.form.patchValue({ doctorName: fullName });
        }
      }
    });
  }

  get medicineLines(): FormArray {
    return this.form.get('medicineLines') as FormArray;
  }

  addLine(): void {
    this.medicineLines.push(this.createLineGroup());
  }

  removeLine(index: number): void {
    if (this.medicineLines.length === 1) {
      return;
    }
    this.medicineLines.removeAt(index);
    delete this.lineSuggestions[index];
  }

  onLineEnter(index: number): void {
    if (index === this.medicineLines.length - 1) {
      this.addLine();
    }
  }

  onMedicineInput(index: number): void {
    const patientId = Number(this.form.get('patientId')?.value);
    const medicineName = String(this.medicineLines.at(index).get('medicationName')?.value || '').trim();

    if (!patientId || patientId <= 0 || medicineName.length < 2) {
      this.lineSuggestions[index] = [];
      return;
    }

    this.pharmacyService.suggestDoctorMedicines(patientId, medicineName).subscribe({
      next: (response) => this.applySuggestionResponse(index, response),
      error: () => {
        this.lineSuggestions[index] = [];
      }
    });
  }

  useSuggestion(index: number, item: DoctorMedicineSuggestionItem): void {
    this.medicineLines.at(index).patchValue({ medicationName: item.medicineName });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.toCreatePayload();

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.createPrescription(payload).subscribe({
      next: (response) => {
        this.saving = false;
        this.successMessage = this.buildSuccessMessage(response);
        this.resetForm();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Failed to create prescription';
      }
    });
  }

  stockBadgeClass(status: string): string {
    if (status === 'IN_STOCK') return 'badge in';
    if (status === 'OUT_OF_STOCK') return 'badge out';
    return 'badge unresolved';
  }

  private createLineGroup(): FormGroup {
    return this.fb.group({
      medicationName: ['', [Validators.required, Validators.minLength(2)]],
      dosage: ['', [Validators.required, Validators.minLength(1)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      instructions: ['']
    });
  }

  private applySuggestionResponse(index: number, response: DoctorMedicineSuggestionResponse): void {
    this.lineSuggestions[index] = response.suggestions || [];
    this.defaultPharmacyResolved = response.hasDefaultPharmacy;
    this.defaultPharmacyName = response.pharmacyName || '';
    this.defaultPharmacyWarning = response.guidanceMessage || '';
  }

  private toCreatePayload(): PrescriptionCreateRequest {
    const lines = this.medicineLines.controls.map(line => ({
      medicationName: String(line.get('medicationName')?.value || '').trim(),
      dosage: String(line.get('dosage')?.value || '').trim(),
      quantity: Number(line.get('quantity')?.value || 0),
      instructions: String(line.get('instructions')?.value || '').trim() || undefined
    }));

    return {
      patientId: Number(this.form.get('patientId')?.value),
      patientName: String(this.form.get('patientName')?.value || '').trim(),
      doctorName: String(this.form.get('doctorName')?.value || '').trim(),
      medicationName: lines[0].medicationName,
      dosage: lines[0].dosage,
      quantity: lines[0].quantity,
      instructions: lines[0].instructions,
      medicineLines: lines
    };
  }

  private buildSuccessMessage(response: PrescriptionResponse): string {
    if (response.assignedToPharmacy === false) {
      return 'Prescription saved. Patient has no default pharmacy yet, so this is pending assignment.';
    }

    if (response.pharmacyName) {
      return `Prescription sent to ${response.pharmacyName}.`;
    }

    return 'Prescription created successfully.';
  }

  private resetForm(): void {
    this.form.patchValue({
      patientId: null,
      patientName: ''
    });

    while (this.medicineLines.length > 1) {
      this.medicineLines.removeAt(this.medicineLines.length - 1);
    }

    this.medicineLines.at(0).reset({
      medicationName: '',
      dosage: '',
      quantity: 1,
      instructions: ''
    });

    this.lineSuggestions = {};
    this.defaultPharmacyResolved = true;
    this.defaultPharmacyName = '';
    this.defaultPharmacyWarning = '';
  }
}
