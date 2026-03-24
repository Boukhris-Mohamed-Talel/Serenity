import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { UserService } from '../../../core/services/user.service';
import {
  DoctorMedicineSuggestionItem,
  DoctorMedicineSuggestionResponse,
  DoctorPatientSuggestionItem,
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

  patientLookupError = '';
  patientMatches: DoctorPatientSuggestionItem[] = [];
  selectedPatientId: number | null = null;
  selectedPatientName = '';
  showPatientMatches = false;

  lineSuggestions: Record<number, DoctorMedicineSuggestionItem[]> = {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly pharmacyService: PharmacyService,
    private readonly userService: UserService
  ) {
    this.form = this.fb.group({
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
    const patientId = this.selectedPatientId;
    const medicineName = String(this.medicineLines.at(index).get('medicationName')?.value || '').trim();

    if (!patientId || medicineName.length < 2) {
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

  onPatientNameInput(): void {
    const query = String(this.form.get('patientName')?.value || '').trim();

    if (query.length < 1) {
      this.patientMatches = [];
      this.showPatientMatches = false;
      this.selectedPatientId = null;
      this.selectedPatientName = '';
      return;
    }

    if (!this.selectedPatientName || this.selectedPatientName.toLowerCase() !== query.toLowerCase()) {
      this.selectedPatientId = null;
    }

    this.patientLookupError = '';
    this.pharmacyService.suggestDoctorPatients(query).subscribe({
      next: (response) => {
        this.patientMatches = response.suggestions || [];
        this.showPatientMatches = true;
      },
      error: (err) => {
        this.patientMatches = [];
        this.showPatientMatches = false;
        this.patientLookupError = err.error?.message || 'Unable to search patients right now.';
      }
    });
  }

  selectPatient(patient: DoctorPatientSuggestionItem): void {
    const displayName = patient.displayName;
    this.selectedPatientId = patient.patientId;
    this.selectedPatientName = displayName;
    this.form.patchValue({ patientName: displayName });
    this.patientMatches = [];
    this.showPatientMatches = false;
  }

  hidePatientMatches(): void {
    setTimeout(() => {
      this.showPatientMatches = false;
    }, 120);
  }

  patientAvatarUrl(patient: DoctorPatientSuggestionItem): string {
    return patient.profilePictureUrl || '';
  }

  getPatientDisplayName(patient: DoctorPatientSuggestionItem): string {
    return patient.displayName;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.selectedPatientId) {
      this.errorMessage = 'Please select a patient from the name list.';
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
      patientId: this.selectedPatientId!,
      patientName: String(this.form.get('patientName')?.value || '').trim(),
      doctorName: String(this.form.get('doctorName')?.value || '').trim(),
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
      patientName: ''
    });

    this.selectedPatientId = null;
    this.selectedPatientName = '';
    this.patientMatches = [];
    this.showPatientMatches = false;

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
