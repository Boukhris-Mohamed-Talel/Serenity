import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { PrescriptionMedication, PrescriptionRequest } from '../../../models/prescription.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-prescription-form',
  templateUrl: './prescription-form.component.html',
  styleUrls: ['./prescription-form.component.scss']
})
export class PrescriptionFormComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  prescriptionId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;

  readonly form = this.fb.group({
    medications: this.fb.array<FormGroup>([
      this.createMedicationGroup()
    ]),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
    private readonly notification: NotificationService
  ) {}

  get medicationsArray(): FormArray<FormGroup> {
    return this.form.controls.medications;
  }

  addMedication(): void {
    this.medicationsArray.push(this.createMedicationGroup());
  }

  removeMedication(index: number): void {
    if (this.medicationsArray.length <= 1) {
      return;
    }
    this.medicationsArray.removeAt(index);
  }

  private createMedicationGroup(initial?: Partial<PrescriptionMedication>): FormGroup {
    return this.fb.group({
      medicationName: [initial?.medicationName ?? '', [Validators.required, Validators.maxLength(100)]],
      dosage: [initial?.dosage ?? '', [Validators.required, Validators.maxLength(50)]],
      frequency: [initial?.frequency ?? '', [Validators.required, Validators.maxLength(50)]],
      quantity: [initial?.quantity ?? 1, [Validators.required, Validators.min(1)]],
      startDate: [initial?.startDate ?? '', [Validators.required]],
      endDate: [initial?.endDate ?? ''],
      status: [initial?.status ?? 'ACTIVE', [Validators.required]],
      instructions: [initial?.instructions ?? '', [Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    const rid = getParamFromRouteTree(this.route, 'recordId');
    if (!pid || !rid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.recordId = +rid;

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'new') {
      this.isEdit = false;
    } else if (path === ':id/edit') {
      this.isEdit = true;
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.prescriptionId = +id;
        this.loadPrescription(this.prescriptionId);
      }
    }
  }

  private loadPrescription(id: number): void {
    this.loading = true;
    this.prescriptionService.getPrescriptionById(id).subscribe({
      next: (p) => {
        this.medicationsArray.clear();
        const meds = p.medications?.length ? p.medications : [{} as PrescriptionMedication];
        meds.forEach(m => this.medicationsArray.push(this.createMedicationGroup({
          medicationName: m.medicationName ?? '',
          dosage: m.dosage ?? '',
          frequency: m.frequency ?? '',
          quantity: m.quantity ?? 1,
          startDate: m.startDate ? m.startDate.substring(0, 10) : '',
          endDate: m.endDate ? m.endDate.substring(0, 10) : '',
          status: m.status ?? 'ACTIVE',
          instructions: m.instructions ?? ''
        })));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        if (this.patientId != null && this.recordId != null) {
          this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        }
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.patientId == null || this.recordId == null) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const medicationsRaw = (v.medications ?? []) as Array<Record<string, unknown>>;
    const medications: PrescriptionMedication[] = medicationsRaw
      .map(m => ({
        medicationName: String(m['medicationName'] ?? '').trim(),
        dosage: String(m['dosage'] ?? '').trim(),
        frequency: String(m['frequency'] ?? '').trim(),
        quantity: Number(m['quantity'] ?? 1),
        startDate: String(m['startDate'] ?? ''),
        endDate: m['endDate'] ? String(m['endDate']) : null,
        status: String(m['status'] ?? 'ACTIVE').trim().toUpperCase() as 'ACTIVE' | 'INACTIVE',
        instructions: String(m['instructions'] ?? '').trim() || null
      }))
      .filter(m => m.medicationName.length > 0);

    if (!medications.length) {
      this.notification.error('Ajoute au moins un médicament.');
      return;
    }

    const body: PrescriptionRequest = {
      medications,
      medicalRecordId: this.recordId,
      patientId: this.patientId
    };

    this.saving = true;
    const req$ =
      this.isEdit && this.prescriptionId
        ? this.prescriptionService.updatePrescription(this.prescriptionId, body)
        : this.prescriptionService.createPrescription(body);

    req$.subscribe({
      next: () => {
        this.notification.success(this.isEdit ? 'Prescription mise à jour' : 'Prescription créée');
        this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.patientId != null && this.recordId != null) {
      this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
    }
  }
}
